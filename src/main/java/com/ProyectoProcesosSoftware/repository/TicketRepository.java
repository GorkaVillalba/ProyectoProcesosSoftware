package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Ticket;
import com.ProyectoProcesosSoftware.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad {@link Ticket}.
 *
 * <p>Al extender {@link JpaRepository} hereda automáticamente las
 * operaciones CRUD básicas; esta interfaz añade los métodos derivados por
 * convención de nombre que necesita el dominio de venta y consulta de
 * entradas (consultar las entradas de un asistente, comprobar si ya posee
 * una entrada para un evento, etc.) y una consulta agregada para el
 * cuadro de mando del organizador (US-28 / T-28.2).</p>
 *
 * <p>Spring Data implementa los métodos derivados en tiempo de arranque a
 * partir de su nombre, sin necesidad de escribir SQL ni JPQL.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see Ticket
 * @see TicketStatus
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Devuelve todas las entradas asociadas a un asistente, sin orden
     * garantizado.
     *
     * @param asistenteId identificador del usuario asistente cuyas entradas
     *                    se desean recuperar.
     * @return lista (posiblemente vacía) de tickets pertenecientes al
     *         asistente indicado; nunca {@code null}.
     */
    List<Ticket> findByAsistenteId(Long asistenteId);

    /**
     * Devuelve todas las entradas de un asistente ordenadas por fecha de
     * compra descendente (la más reciente primero).
     *
     * <p>Pensado para la vista "Mis entradas" del asistente, donde resulta
     * útil ver primero las compras más recientes.</p>
     *
     * @param asistenteId identificador del usuario asistente cuyas entradas
     *                    se desean recuperar.
     * @return lista (posiblemente vacía) de tickets del asistente, ordenada
     *         por {@code fechaCompra DESC}; nunca {@code null}.
     */
    List<Ticket> findByAsistenteIdOrderByFechaCompraDesc(Long asistenteId);

    /**
     * Devuelve todas las entradas vendidas para un evento concreto.
     *
     * <p>Útil para los flujos del organizador (listar asistentes,
     * estadísticas, validación en puerta, etc.).</p>
     *
     * @param eventoId identificador del evento cuyas entradas se desean
     *                 recuperar.
     * @return lista (posiblemente vacía) de tickets asociados al evento;
     *         nunca {@code null}.
     */
    List<Ticket> findByEventoId(Long eventoId);

    /**
     * Indica si existe alguna entrada (en cualquier estado) que vincule a
     * un asistente con un evento.
     *
     * @param eventoId    identificador del evento.
     * @param asistenteId identificador del asistente.
     * @return {@code true} si existe al menos una entrada que cumpla el
     *         criterio; {@code false} en caso contrario.
     */
    boolean existsByEventoIdAndAsistenteId(Long eventoId, Long asistenteId);

    /**
     * Indica si existe alguna entrada en un estado concreto que vincule a
     * un asistente con un evento.
     *
     * <p>Lo utiliza {@code TicketService} para impedir que un mismo
     * asistente compre dos veces la misma entrada: se comprueba la
     * existencia de un ticket con {@link TicketStatus#VALIDO} antes de
     * permitir una nueva compra.</p>
     *
     * @param eventoId    identificador del evento.
     * @param asistenteId identificador del asistente.
     * @param estado      estado del ticket que se quiere comprobar
     *                    (por ejemplo, {@link TicketStatus#VALIDO}).
     * @return {@code true} si existe al menos una entrada del asistente
     *         para el evento en el estado indicado; {@code false} en caso
     *         contrario.
     */
    boolean existsByEventoIdAndAsistenteIdAndEstado(
            Long eventoId, Long asistenteId, TicketStatus estado);

    // US-28 / T-28.2: suma de precioFinal de tickets VALIDO del organizador.

    /**
     * Calcula los ingresos reales de un organizador sumando el
     * {@code precioFinal} de todas las entradas en estado
     * {@link TicketStatus#VALIDO} de sus eventos.
     *
     * <p>A diferencia de
     * {@code EventoRepository.sumIngresosByOrganizadorId(...)} —que estima
     * los ingresos a partir del precio base y del número de vendidas—,
     * esta consulta utiliza el precio real cobrado en cada ticket, por lo
     * que <strong>refleja correctamente el efecto del precio dinámico</strong>
     * (Regular ×1.25, LastMinute ×1.50) en los ingresos.</p>
     *
     * <p>Solo se contabilizan las entradas en estado {@code VALIDO}, de
     * modo que las cancelaciones (estado {@code CANCELADO}) no se suman,
     * lo cual es coherente con la realidad contable: una entrada cancelada
     * suele implicar un reembolso y por tanto no genera ingreso neto.</p>
     *
     * <p>La consulta utiliza {@code COALESCE(SUM(...), 0)} para garantizar
     * que la respuesta nunca sea {@code null} cuando el organizador no
     * tiene ventas válidas.</p>
     *
     * @param organizadorId identificador del organizador cuyos ingresos se
     *                      desean calcular.
     * @return suma de {@code precioFinal} de los tickets válidos de sus
     *         eventos; {@link java.math.BigDecimal#ZERO} si no hay ninguno.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(t.precioFinal), 0) " +
        "FROM Ticket t " +
        "WHERE t.evento.organizador.id = :organizadorId " +
        "AND t.estado = com.ProyectoProcesosSoftware.model.TicketStatus.VALIDO")
    java.math.BigDecimal sumarIngresosByOrganizadorId(
            @org.springframework.data.repository.query.Param("organizadorId") Long organizadorId);


}