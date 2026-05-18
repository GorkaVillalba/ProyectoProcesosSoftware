package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.pricing.PricingContext;
import com.ProyectoProcesosSoftware.dto.*;
import com.ProyectoProcesosSoftware.exception.*;
import com.ProyectoProcesosSoftware.model.*;
import com.ProyectoProcesosSoftware.repository.EventoRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

// ═══════════════════════════════════════════════════════════════
// T-16 (Persona 4): Servicio de Eventos
// Incluye métodos de T-18 (listar), T-20 (detalle), T-22 (editar), T-24 (eliminar)
// ═══════════════════════════════════════════════════════════════

/**
 * Servicio de aplicación encargado de la gestión integral del ciclo de vida
 * de los eventos de la plataforma.
 *
 * <p>Esta clase concentra la lógica de negocio relacionada con la creación,
 * consulta, edición y eliminación de eventos, así como con el cálculo del
 * precio dinámico en función de la estrategia de pricing vigente
 * ({@link PricingContext}). Actúa como capa intermedia entre los controladores
 * REST y los repositorios de persistencia, garantizando:</p>
 *
 * <ul>
 *   <li>La validación de las reglas de negocio (autoría, aforo, fechas, etc.).</li>
 *   <li>La verificación de la autorización del usuario que invoca cada operación
 *       (solo los organizadores pueden crear, editar o eliminar sus eventos).</li>
 *   <li>La traducción entre entidades de dominio ({@link Evento}) y los DTOs
 *       expuestos a la capa de presentación.</li>
 *   <li>La aplicación transaccional de los cambios sobre la base de datos
 *       mediante {@link Transactional}.</li>
 * </ul>
 *
 * <p>Las operaciones expuestas cubren las historias de usuario T-16 (creación),
 * T-18 (listado), T-20 (detalle), T-22 (edición) y T-24 (eliminación).</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see EventoRepository
 * @see PricingContext
 * @see EventoMapper
 */
@Service
public class EventoService {
    @Autowired
    private PricingContext pricingContext;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crea y persiste un nuevo evento publicado por un organizador.
     *
     * <p>El método verifica que el usuario indicado exista y tenga rol
     * {@link Rol#ORGANIZADOR}. En caso afirmativo, construye una nueva
     * entidad {@link Evento} a partir del DTO recibido, la marca como
     * {@link EstadoEvento#PUBLICADO} y la guarda en el repositorio.</p>
     *
     * @param dto             objeto con los datos del evento a crear
     *                        (nombre, descripción, fecha, hora, ubicación,
     *                        aforo máximo y precio base). No puede ser {@code null}.
     * @param organizadorId   identificador único del usuario que actúa como
     *                        organizador del evento. No puede ser {@code null}.
     * @return un {@link EventoResponseDTO} con la información del evento
     *         recién creado, incluyendo el precio calculado por la
     *         estrategia de pricing vigente.
     * @throws ResourceNotFoundException   si no existe ningún usuario con el
     *                                     identificador {@code organizadorId}.
     * @throws UnauthorizedActionException si el usuario existe pero su rol no
     *                                     es {@link Rol#ORGANIZADOR} y, por
     *                                     tanto, no está autorizado a crear
     *                                     eventos.
     */
    @Transactional
    public EventoResponseDTO crearEvento(CrearEventoDTO dto, Long organizadorId) {
        Usuario org = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (org.getRol() != Rol.ORGANIZADOR) {
            throw new UnauthorizedActionException("Solo los organizadores pueden crear eventos");
        }

        Evento evento = new Evento();
        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(dto.getFecha());
        evento.setHora(dto.getHora());
        evento.setUbicacion(dto.getUbicacion());
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setPrecioBase(dto.getPrecioBase());
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(org);

        return EventoMapper.toResponseDTO(eventoRepository.save(evento), pricingContext);
    }

    // T-18 (Persona 5)
    /**
     * Recupera la lista paginada de eventos publicados aplicando los filtros
     * opcionales de nombre y ubicación.
     *
     * <p>Solo se devuelven eventos cuyo estado es {@link EstadoEvento#PUBLICADO}.
     * Tanto el filtro de nombre como el de ubicación son opcionales: si se
     * pasa {@code null} o cadena vacía, no se aplica filtrado por ese campo.
     * Para cada evento se calcula su precio actual conforme a la estrategia
     * de pricing vigente.</p>
     *
     * @param nombre    cadena a buscar (parcial) dentro del nombre del evento;
     *                  puede ser {@code null} si no se desea filtrar por nombre.
     * @param ubicacion cadena a buscar (parcial) dentro de la ubicación del
     *                  evento; puede ser {@code null} si no se desea filtrar
     *                  por ubicación.
     * @param pageable  información de paginación y ordenación a aplicar sobre
     *                  el resultado. No puede ser {@code null}.
     * @return una {@link Page} de {@link EventoResponseDTO} con los eventos
     *         publicados que cumplen los filtros indicados.
     */
    public Page<EventoResponseDTO> listarEventos(String nombre, String ubicacion, Pageable pageable) {
        return eventoRepository.findByEstadoAndFiltros(EstadoEvento.PUBLICADO, nombre, ubicacion, pageable)
                .map(evento -> EventoMapper.toResponseDTO(evento, pricingContext));
    }

    // T-20 (Persona 1)
    /**
     * Obtiene el detalle completo de un evento concreto identificado por su id.
     *
     * <p>Se recupera la entidad correspondiente y se transforma en su
     * representación de salida ({@link EventoResponseDTO}), incluyendo el
     * precio calculado según la estrategia de pricing aplicable en ese momento.</p>
     *
     * @param id identificador único del evento a consultar. No puede ser
     *           {@code null}.
     * @return un {@link EventoResponseDTO} con la información detallada del
     *         evento solicitado.
     * @throws ResourceNotFoundException si no existe ningún evento con el
     *                                   identificador indicado.
     */
    public EventoResponseDTO obtenerDetalle(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
        return EventoMapper.toResponseDTO(evento, pricingContext);
    }

    // T-22 (Persona 6)
    /**
     * Edita los datos principales de un evento existente, validando la
     * autoría del organizador y un conjunto de reglas de negocio.
     *
     * <p>Las reglas aplicadas son:</p>
     * <ul>
     *   <li>Solo el organizador que creó el evento puede modificarlo.</li>
     *   <li>El nuevo aforo máximo nunca puede ser inferior al número de
     *       entradas ya vendidas.</li>
     *   <li>Únicamente se admite establecer una fecha pasada cuando el
     *       evento ya se encuentra en estado {@link EstadoEvento#FINALIZADO}
     *       (caso de corrección histórica, requisito US-20 / T-20.3).</li>
     * </ul>
     *
     * <p>Si todas las validaciones se superan, se actualizan los campos
     * editables del evento y se persisten los cambios.</p>
     *
     * @param eventoId      identificador del evento a editar. No puede ser
     *                      {@code null}.
     * @param dto           DTO con los nuevos valores de los atributos
     *                      editables (nombre, descripción, fecha, hora,
     *                      ubicación, aforo máximo y precio base). No puede
     *                      ser {@code null}.
     * @param organizadorId identificador del usuario que solicita la edición;
     *                      debe coincidir con el organizador propietario del
     *                      evento. No puede ser {@code null}.
     * @return un {@link EventoResponseDTO} con el estado del evento tras la
     *         actualización.
     * @throws ResourceNotFoundException   si no existe ningún evento con el
     *                                     identificador {@code eventoId}.
     * @throws UnauthorizedActionException si el usuario indicado no es el
     *                                     organizador que creó el evento.
     * @throws BusinessRuleException       si el nuevo aforo es menor que las
     *                                     entradas vendidas, o si se intenta
     *                                     asignar una fecha pasada a un evento
     *                                     que no esté en estado
     *                                     {@link EstadoEvento#FINALIZADO}.
     */
    @Transactional
    public EventoResponseDTO editarEvento(Long eventoId, EditarEventoDTO dto, Long organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        if (!evento.getOrganizador().getId().equals(organizadorId)) {
            throw new UnauthorizedActionException("Solo el organizador creador puede editar este evento");
        }
        if (dto.getAforoMaximo() < evento.getEntradasVendidas()) {
            throw new BusinessRuleException("No se puede reducir el aforo por debajo de las entradas vendidas");
        }

        // US-20 / T-20.3: solo se permite asignar una fecha pasada cuando el
        // evento ya está FINALIZADO (caso de corrección histórica).
        if (dto.getFecha().isBefore(LocalDate.now())
                && evento.getEstado() != EstadoEvento.FINALIZADO) {
            throw new BusinessRuleException(
                    "Solo se puede asignar una fecha pasada a un evento ya FINALIZADO");
        }

        evento.setNombre(dto.getNombre());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFecha(dto.getFecha());
        evento.setHora(dto.getHora());
        evento.setUbicacion(dto.getUbicacion());
        evento.setAforoMaximo(dto.getAforoMaximo());
        evento.setPrecioBase(dto.getPrecioBase());

        return EventoMapper.toResponseDTO(eventoRepository.save(evento), pricingContext);
    }

    /**
     * Calcula el precio actual de un evento aplicando la estrategia de
     * pricing dinámico vigente, junto con información complementaria sobre
     * el nivel de incremento y el porcentaje de ocupación.
     *
     * <p>El método obtiene el número de entradas vendidas y el aforo del
     * evento, delega en {@link PricingContext} para determinar la estrategia
     * aplicable y el precio resultante, y traduce el nombre interno de la
     * estrategia a un nivel descriptivo:</p>
     * <ul>
     *   <li>{@code "Regular"}    → {@code "+25%"}</li>
     *   <li>{@code "LastMinute"} → {@code "+50%"}</li>
     *   <li>Cualquier otra       → {@code "Base"}</li>
     * </ul>
     *
     * @param id identificador único del evento cuyo precio se desea calcular.
     *           No puede ser {@code null}.
     * @return un {@link PrecioEventoDTO} con el identificador del evento,
     *         el precio base, el precio actual calculado, el nombre de la
     *         estrategia aplicada, el nivel descriptivo y el porcentaje de
     *         ocupación.
     * @throws ResourceNotFoundException si no existe ningún evento con el
     *                                   identificador indicado.
     */
    public PrecioEventoDTO obtenerPrecio(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));

        int vendidas = evento.getEntradasVendidas();
        int aforo = evento.getAforoMaximo();
        int pctOcupacion = aforo > 0 ? (int) ((double) vendidas / aforo * 100) : 0;

        String estrategia = pricingContext.nombreEstrategia(vendidas, aforo);
        String nivel;
        switch (estrategia) {
            case "Regular":    nivel = "+25%"; break;
            case "LastMinute": nivel = "+50%"; break;
            default:           nivel = "Base"; break;
        }

        PrecioEventoDTO dto = new PrecioEventoDTO();
        dto.setEventoId(evento.getId());
        dto.setPrecioBase(evento.getPrecioBase());
        dto.setPrecioActual(pricingContext.calcularPrecio(evento.getPrecioBase(), vendidas, aforo));
        dto.setEstrategia(estrategia);
        dto.setNivel(nivel);
        dto.setPorcentajeOcupacion(pctOcupacion);
        return dto;
    }

    // T-24 (Persona 6)
    /**
     * Elimina permanentemente un evento del sistema, siempre que se cumplan
     * las condiciones de autoría y de ausencia de ventas.
     *
     * <p>Las reglas de negocio aplicadas son:</p>
     * <ul>
     *   <li>Solo el organizador que creó el evento puede eliminarlo.</li>
     *   <li>No se permite eliminar un evento que ya tenga entradas vendidas,
     *       para preservar la trazabilidad de las compras realizadas por los
     *       asistentes.</li>
     * </ul>
     *
     * @param eventoId      identificador del evento a eliminar. No puede ser
     *                      {@code null}.
     * @param organizadorId identificador del usuario que solicita la
     *                      eliminación; debe coincidir con el organizador
     *                      propietario del evento. No puede ser {@code null}.
     * @throws ResourceNotFoundException   si no existe ningún evento con el
     *                                     identificador {@code eventoId}.
     * @throws UnauthorizedActionException si el usuario indicado no es el
     *                                     organizador que creó el evento.
     * @throws BusinessRuleException       si el evento tiene una o más
     *                                     entradas vendidas y, por tanto, no
     *                                     puede ser eliminado.
     */
    @Transactional
    public void eliminarEvento(Long eventoId, Long organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
        if (!evento.getOrganizador().getId().equals(organizadorId)) {
            throw new UnauthorizedActionException("Solo el organizador creador puede eliminar este evento");
        }
        if (evento.getEntradasVendidas() > 0) {
            throw new BusinessRuleException("No se puede eliminar: tiene " + evento.getEntradasVendidas() + " entradas vendidas");
        }
        eventoRepository.delete(evento);
    }

}
