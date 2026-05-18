package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.TicketMapper;
import com.ProyectoProcesosSoftware.dto.TicketResponseDTO;
import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.TicketRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de aplicación encargado de la gestión integral de las entradas
 * (tickets) asociadas a los eventos de la plataforma.
 *
 * <p>Esta clase concentra la lógica de negocio relacionada con la compra,
 * cancelación y consulta de entradas por parte de los usuarios con rol
 * {@link Rol#ASISTENTE}. Actúa como capa intermedia entre los controladores
 * REST y los repositorios de persistencia, garantizando:</p>
 *
 * <ul>
 *   <li>La validación de las reglas de negocio relativas al ciclo de vida
 *       del ticket (estado del evento, aforo, unicidad de la compra,
 *       ventana mínima para cancelar, etc.).</li>
 *   <li>La verificación de la autorización del usuario que invoca cada
 *       operación (solo los asistentes pueden comprar; solo el comprador
 *       puede cancelar su propia entrada).</li>
 *   <li>La aplicación del precio dinámico vigente en el instante de la
 *       compra mediante {@link PricingContext}, de modo que el precio
 *       almacenado en el ticket refleja la estrategia activa
 *       (Base / Regular / LastMinute) según el porcentaje de ocupación
 *       del evento.</li>
 *   <li>El control de concurrencia mediante <em>bloqueo optimista</em>
 *       sobre la entidad {@link Evento}: si dos asistentes intentan
 *       reservar la última plaza simultáneamente, solo uno de ellos
 *       finaliza la compra y al otro se le devuelve un error de negocio
 *       accionable (US-18).</li>
 *   <li>La actualización transaccional de los contadores de
 *       {@code entradasVendidas} y del estado del evento
 *       ({@link EstadoEvento#PUBLICADO} ↔ {@link EstadoEvento#AGOTADO}).</li>
 * </ul>
 *
 * <h2>Reglas de negocio destacadas</h2>
 *
 * <p><b>Regla de las 48 horas (cancelación).</b> Una entrada solo puede
 * cancelarse si faltan <em>más</em> de 48 horas para el inicio del evento.
 * Pasado ese umbral, la cancelación se rechaza con
 * {@link BusinessRuleException} para proteger al organizador frente a
 * cancelaciones de última hora que no podrían ser reasignadas. La cuenta
 * atrás se calcula a partir de la fecha y hora exactas del evento.</p>
 *
 * <p><b>Precio dinámico.</b> El precio final que paga el asistente se
 * calcula en el momento de la compra a partir del precio base del evento
 * y del porcentaje de ocupación, mediante la estrategia que devuelva
 * {@link PricingContext} (patrón <em>Strategy</em>):</p>
 * <ul>
 *   <li><b>Base</b>: ocupación baja, precio sin recargo.</li>
 *   <li><b>Regular</b>: ocupación media, recargo del 25 % sobre el precio base.</li>
 *   <li><b>LastMinute</b>: ocupación alta, recargo del 50 % sobre el precio base.</li>
 * </ul>
 * <p>El nombre de la estrategia aplicada se devuelve en el
 * {@link TicketResponseDTO} para ofrecer trazabilidad al cliente sobre
 * por qué se ha cobrado un determinado precio.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see TicketRepository
 * @see EventoRepository
 * @see PricingContext
 * @see TicketMapper
 */
@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PricingContext pricingContext;

        // US-17: comprar entrada con precio calculado en el momento de la compra
    /**
     * Compra una entrada para un evento publicado, aplicando el precio
     * dinámico vigente y controlando la concurrencia sobre el aforo.
     *
     * <p>El flujo realiza, en este orden, las siguientes verificaciones y
     * acciones:</p>
     * <ol>
     *   <li>Comprueba que el evento existe.</li>
     *   <li>Rechaza la compra si el evento está {@link EstadoEvento#AGOTADO}
     *       o si no se encuentra en estado {@link EstadoEvento#PUBLICADO}.</li>
     *   <li>Comprueba que aún quedan plazas disponibles en el aforo.</li>
     *   <li>Comprueba que el usuario existe y que tiene rol
     *       {@link Rol#ASISTENTE}.</li>
     *   <li>Comprueba que el asistente no posee ya un ticket
     *       {@link TicketStatus#VALIDO} para el mismo evento (unicidad).</li>
     *   <li>Calcula el precio final mediante {@link PricingContext} a partir
     *       del precio base y del porcentaje de ocupación actual.</li>
     *   <li>Incrementa el contador de entradas vendidas y, si se alcanza el
     *       aforo, transiciona el evento a {@link EstadoEvento#AGOTADO}.</li>
     *   <li>Persiste el evento con <em>flush</em> inmediato (US-18). Si otro
     *       hilo modificó el evento entre la lectura y este flush, Hibernate
     *       lanza {@link ObjectOptimisticLockingFailureException} y este
     *       servicio la traduce a {@link BusinessRuleException} con un
     *       mensaje accionable.</li>
     *   <li>Guarda el ticket y lo devuelve junto con el nombre de la
     *       estrategia de precio aplicada.</li>
     * </ol>
     *
     * @param eventoId    identificador del evento sobre el que se desea
     *                    comprar la entrada. No puede ser {@code null}.
     * @param asistenteId identificador del usuario comprador, que debe tener
     *                    rol {@link Rol#ASISTENTE}. No puede ser {@code null}.
     * @return un {@link TicketResponseDTO} con los datos del ticket creado,
     *         incluyendo el precio final cobrado y el nombre de la estrategia
     *         de pricing aplicada en el instante de la compra.
     * @throws ResourceNotFoundException   si no existe el evento o el usuario
     *                                     indicados.
     * @throws UnauthorizedActionException si el usuario existe pero su rol no
     *                                     es {@link Rol#ASISTENTE}.
     * @throws BusinessRuleException       si el evento está agotado, no está
     *                                     publicado, no quedan plazas, el
     *                                     asistente ya tiene una entrada
     *                                     válida para ese evento, o si se
     *                                     produce un conflicto de bloqueo
     *                                     optimista al competir por la última
     *                                     plaza con otro comprador.
     */
    @Transactional
    public TicketResponseDTO comprarEntrada(Long eventoId, Long asistenteId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventoId));
        if (evento.getEstado() == EstadoEvento.AGOTADO) {
            throw new BusinessRuleException("El evento está agotado");
        }
        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new BusinessRuleException("Solo se pueden comprar entradas de eventos publicados");
        }

        if (evento.getEntradasVendidas() >= evento.getAforoMaximo()) {
            throw new BusinessRuleException("No quedan plazas disponibles para este evento");
        }

        Usuario asistente = usuarioRepository.findById(asistenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + asistenteId));

        if (asistente.getRol() != Rol.ASISTENTE) {
            throw new UnauthorizedActionException("Solo los asistentes pueden comprar entradas");
        }

        if (ticketRepository.existsByEventoIdAndAsistenteIdAndEstado(
                eventoId, asistenteId, TicketStatus.VALIDO)) {
            throw new BusinessRuleException("Ya tienes una entrada para este evento");
        }

        BigDecimal precioFinal = pricingContext.calcularPrecio(
                evento.getPrecioBase(),
                evento.getEntradasVendidas(),
                evento.getAforoMaximo()
        );
        String estrategia = pricingContext.nombreEstrategia(
                evento.getEntradasVendidas(),
                evento.getAforoMaximo()
        );

        Ticket ticket = new Ticket();
        ticket.setEvento(evento);
        ticket.setAsistente(asistente);
        ticket.setPrecioFinal(precioFinal);

        evento.setEntradasVendidas(evento.getEntradasVendidas() + 1);
        if (evento.getEntradasVendidas() >= evento.getAforoMaximo()) {
            evento.setEstado(EstadoEvento.AGOTADO);
        }

        // US-18: bloqueo optimista. Si otro hilo modifica el evento entre la
        // lectura y este flush, Hibernate lanza ObjectOptimisticLockingFailureException
        // y devolvemos 409 con un mensaje accionable para el cliente.
        try {
            eventoRepository.saveAndFlush(evento);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException(
                    "La plaza acaba de ser ocupada por otro usuario, inténtalo de nuevo");
        }

        Ticket guardado = ticketRepository.save(ticket);
        return TicketMapper.TicketResponseDTO(guardado, estrategia);
    }

    // T-13 + T-15: cancelar entrada con regla de 48h y liberar plaza
    /**
     * Cancela una entrada previamente comprada y libera la plaza en el aforo,
     * aplicando la regla de las 48 horas y restituyendo el estado del evento
     * si procede.
     *
     * <p>El método realiza las siguientes validaciones y efectos:</p>
     * <ol>
     *   <li>Comprueba que la entrada existe.</li>
     *   <li>Comprueba que el usuario solicitante es el comprador original;
     *       en caso contrario, lanza {@link UnauthorizedActionException}.</li>
     *   <li>Comprueba que la entrada está en estado
     *       {@link TicketStatus#VALIDO}; las entradas ya canceladas no se
     *       pueden volver a cancelar.</li>
     *   <li><b>Regla de las 48 horas (T-13 / T-15)</b>: calcula los minutos
     *       que faltan hasta la fecha y hora del evento y rechaza la
     *       cancelación si el margen es igual o inferior a {@code 48 * 60}
     *       minutos. Esto evita cancelaciones de última hora que el
     *       organizador no podría reasignar.</li>
     *   <li>Marca el ticket como {@link TicketStatus#CANCELADO}.</li>
     *   <li>Decrementa el contador de entradas vendidas del evento (sin
     *       bajar de cero).</li>
     *   <li>Si el evento estaba {@link EstadoEvento#AGOTADO} y tras la
     *       liberación vuelven a quedar plazas, lo devuelve a
     *       {@link EstadoEvento#PUBLICADO}.</li>
     *   <li>Persiste los cambios sobre evento y ticket, y devuelve el
     *       ticket actualizado junto con la estrategia de precio vigente
     *       tras la liberación.</li>
     * </ol>
     *
     * @param ticketId  identificador del ticket que se desea cancelar.
     *                  No puede ser {@code null}.
     * @param usuarioId identificador del usuario que solicita la cancelación;
     *                  debe coincidir con el comprador original del ticket.
     *                  No puede ser {@code null}.
     * @return un {@link TicketResponseDTO} con el ticket en estado
     *         {@link TicketStatus#CANCELADO} y el nombre de la estrategia de
     *         pricing vigente tras la liberación de la plaza.
     * @throws ResourceNotFoundException   si no existe ningún ticket con el
     *                                     identificador indicado.
     * @throws UnauthorizedActionException si el usuario solicitante no es el
     *                                     comprador del ticket.
     * @throws BusinessRuleException       si el ticket ya estaba cancelado o
     *                                     si faltan 48 horas o menos para el
     *                                     inicio del evento.
     */
    @Transactional
    public TicketResponseDTO cancelarEntrada(Long ticketId, Long usuarioId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada no encontrada con id: " + ticketId));

        if (!ticket.getAsistente().getId().equals(usuarioId)) {
            throw new UnauthorizedActionException("No puedes cancelar una entrada que no es tuya");
        }

        if (ticket.getEstado() != TicketStatus.VALIDO) {
            throw new BusinessRuleException("La entrada ya está cancelada");
        }

        Evento evento = ticket.getEvento();
        java.time.LocalDateTime fechaEvento =
                java.time.LocalDateTime.of(evento.getFecha(), evento.getHora());
        long minutosHastaEvento = java.time.Duration.between(
                java.time.LocalDateTime.now(), fechaEvento).toMinutes();
        if (minutosHastaEvento <= 48 * 60) {
            throw new BusinessRuleException(
                    "No se puede cancelar: faltan menos de 48h para el evento");
        }

        ticket.setEstado(TicketStatus.CANCELADO);

        evento.setEntradasVendidas(Math.max(0, evento.getEntradasVendidas() - 1));
        if (evento.getEstado() == EstadoEvento.AGOTADO
                && evento.getEntradasVendidas() < evento.getAforoMaximo()) {
            evento.setEstado(EstadoEvento.PUBLICADO);
        }
        eventoRepository.save(evento);

        Ticket guardado = ticketRepository.save(ticket);
        String estrategia = pricingContext.nombreEstrategia(
                evento.getEntradasVendidas(), evento.getAforoMaximo());
        return TicketMapper.TicketResponseDTO(guardado, estrategia);
    }

    /**
     * Devuelve todas las entradas asociadas a un asistente, sin orden
     * garantizado, junto con la estrategia de pricing vigente para el
     * evento de cada una.
     *
     * <p>Para cada ticket recuperado se invoca {@link PricingContext} a fin
     * de obtener el nombre de la estrategia actualmente aplicable al
     * evento al que pertenece, calculada sobre el porcentaje de ocupación
     * en el instante de la consulta. Este valor se adjunta al
     * {@link TicketResponseDTO} a efectos informativos; no modifica el
     * precio ya pagado, que queda fijado en el momento de la compra.</p>
     *
     * @param asistenteId identificador del usuario cuyas entradas se desean
     *                    consultar. No puede ser {@code null}.
     * @return una {@link List} de {@link TicketResponseDTO} con todas las
     *         entradas del asistente (tanto válidas como canceladas).
     *         Devuelve una lista vacía si el asistente no posee entradas.
     */
    public List<TicketResponseDTO> misEntradas(Long asistenteId) {
        return ticketRepository.findByAsistenteId(asistenteId)
                .stream()
                .map(t -> TicketMapper.TicketResponseDTO(t, pricingContext.nombreEstrategia(
                        t.getEvento().getEntradasVendidas(),
                        t.getEvento().getAforoMaximo())))
                .toList();
    }

    // Consultar mis entradas ordenadas por fecha de compra descendente
    /**
     * Devuelve las entradas de un usuario ordenadas por fecha de compra
     * descendente (compra más reciente primero).
     *
     * <p>Variante de {@link #misEntradas(Long)} que aplica un orden estable
     * por fecha de compra, pensada para la pantalla de "Mis entradas" del
     * asistente, donde resulta más útil ver las compras recientes en la
     * parte superior. Al igual que {@link #misEntradas(Long)}, anexa el
     * nombre de la estrategia de pricing vigente para cada evento a efectos
     * meramente informativos.</p>
     *
     * @param usuarioId identificador del usuario cuyas entradas se desean
     *                  consultar. No puede ser {@code null}.
     * @return una {@link List} de {@link TicketResponseDTO} ordenada por
     *         fecha de compra descendente. Devuelve una lista vacía si el
     *         usuario no posee entradas.
     */
    public List<TicketResponseDTO> getMisEntradas(Long usuarioId) {
        return ticketRepository.findByAsistenteIdOrderByFechaCompraDesc(usuarioId)
                .stream()
                .map(t -> TicketMapper.TicketResponseDTO(t, pricingContext.nombreEstrategia(
                        t.getEvento().getEntradasVendidas(),
                        t.getEvento().getAforoMaximo())))
                .toList();
    }
}