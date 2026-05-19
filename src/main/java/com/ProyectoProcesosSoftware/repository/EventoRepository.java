package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.model.EstadoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

// T-15 (Persona 3): EventoRepository

/**
 * Repositorio Spring Data JPA para la entidad {@link Evento}.
 *
 * <p>Al extender {@link JpaRepository} hereda automáticamente las
 * operaciones CRUD básicas ({@code save}, {@code findById},
 * {@code findAll}, {@code delete}, paginación, etc.), por lo que esta
 * interfaz solo define las consultas específicas del dominio:</p>
 * <ul>
 *   <li>Búsqueda paginada con filtros opcionales para el catálogo público
 *       de eventos (T-15, T-18).</li>
 *   <li>Consultas agregadas para el cuadro de mando del organizador
 *       (US-28 / T-28.2): listado de eventos, suma de entradas vendidas,
 *       ingresos totales, ocupación media y número de eventos.</li>
 * </ul>
 *
 * <p>El repositorio se inyecta principalmente desde
 * {@code EventoService} y desde el servicio de estadísticas del
 * organizador.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see Evento
 * @see EstadoEvento
 */
public interface EventoRepository extends JpaRepository<Evento, Long> {

    /**
     * Devuelve la página de eventos cuyo {@link EstadoEvento} coincide con
     * el indicado y que, opcionalmente, contienen los fragmentos de
     * {@code nombre} y {@code ubicacion} solicitados.
     *
     * <p>Las búsquedas por nombre y ubicación se realizan mediante
     * <code>LIKE '%valor%'</code> insensible a mayúsculas/minúsculas
     * gracias a {@code LOWER(...)}. Cualquiera de los dos filtros puede
     * recibirse como {@code null}, en cuyo caso esa cláusula se considera
     * verdadera y no restringe el resultado.</p>
     *
     * <p>El resultado se entrega paginado conforme al {@link Pageable}
     * recibido, lo que permite a la capa de presentación consumir grandes
     * catálogos de eventos sin sobrecargar la base de datos ni la red.</p>
     *
     * @param estado    estado del evento por el que filtrar (típicamente
     *                  {@link EstadoEvento#PUBLICADO} en el listado público).
     *                  Es obligatorio.
     * @param nombre    fragmento (case-insensitive) que debe contener el
     *                  nombre del evento; {@code null} para no filtrar.
     * @param ubicacion fragmento (case-insensitive) que debe contener la
     *                  ubicación del evento; {@code null} para no filtrar.
     * @param pageable  configuración de paginación y ordenación a aplicar.
     *                  No puede ser {@code null}.
     * @return una {@link Page} de {@link Evento} con los eventos que
     *         cumplen los criterios; nunca {@code null}.
     */
    @Query("SELECT e FROM Evento e WHERE e.estado = :estado " +
           "AND (:nombre IS NULL OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
           "AND (:ubicacion IS NULL OR LOWER(e.ubicacion) LIKE LOWER(CONCAT('%', :ubicacion, '%')))")
    Page<Evento> findByEstadoAndFiltros(
            @Param("estado") EstadoEvento estado,
            @Param("nombre") String nombre,
            @Param("ubicacion") String ubicacion,
            Pageable pageable);

    // US-28 / T-28.2: listado de eventos de un organizador (cálculos en servicio)

    /**
     * Devuelve todos los eventos creados por un organizador concreto, sin
     * paginar y sin orden garantizado.
     *
     * <p>Se utiliza desde el cuadro de mando del organizador (US-28 /
     * T-28.2). Los cálculos derivados (precio dinámico, porcentaje de
     * ocupación, etc.) se realizan en la capa de servicio sobre la lista
     * devuelta por este método, manteniendo el repositorio centrado en
     * el acceso a datos.</p>
     *
     * @param organizadorId identificador del usuario organizador cuyos
     *                      eventos se desean recuperar.
     * @return lista (posiblemente vacía) de eventos del organizador;
     *         nunca {@code null}.
     */
    List<Evento> findByOrganizadorId(Long organizadorId);

    /**
     * Calcula la suma total de entradas vendidas en todos los eventos de
     * un organizador.
     *
     * <p>La consulta utiliza {@code COALESCE(SUM(...), 0)} para garantizar
     * que la respuesta nunca sea {@code null}: si el organizador no tiene
     * eventos, el resultado es {@code 0L}, lo que evita comprobaciones de
     * nulidad en la capa de servicio.</p>
     *
     * @param organizadorId identificador del organizador.
     * @return suma total de entradas vendidas; {@code 0L} si el
     *         organizador no tiene eventos.
     */
    @Query("SELECT COALESCE(SUM(e.entradasVendidas), 0) FROM Evento e WHERE e.organizador.id = :organizadorId")
    Long sumEntradasVendidasByOrganizadorId(@Param("organizadorId") Long organizadorId);

    /**
     * Calcula los ingresos totales aproximados de un organizador como
     * <code>SUM(entradasVendidas × precioBase)</code>.
     *
     * <p>Se utiliza {@code precioBase} como aproximación del precio real
     * cobrado por entrada: no se reconstruye el precio dinámico histórico
     * (Regular / LastMinute) porque la base de datos no almacena el
     * multiplicador aplicado en cada compra individual a este nivel. Si
     * en el futuro se necesita un cálculo exacto, debe agregarse sobre
     * la tabla de tickets (ver {@code TicketRepository.sumarIngresosByOrganizadorId}).</p>
     *
     * <p>La consulta utiliza {@code COALESCE(..., 0)} para evitar valores
     * {@code null} cuando el organizador no tiene eventos.</p>
     *
     * @param organizadorId identificador del organizador.
     * @return ingresos totales aproximados en euros; {@link BigDecimal#ZERO}
     *         si el organizador no tiene eventos.
     */
    @Query("SELECT COALESCE(SUM(e.entradasVendidas * e.precioBase), 0) FROM Evento e WHERE e.organizador.id = :organizadorId")
    BigDecimal sumIngresosByOrganizadorId(@Param("organizadorId") Long organizadorId);

    /**
     * Calcula el porcentaje medio de ocupación de los eventos de un
     * organizador como
     * <code>AVG(entradasVendidas / aforoMaximo × 100)</code>.
     *
     * <p>Solo se incluyen en la media los eventos con
     * {@code aforoMaximo > 0}, descartando los casos degenerados de aforo
     * nulo o negativo que producirían división por cero.</p>
     *
     * <p>Si el organizador no tiene ningún evento que cumpla la condición
     * anterior, la consulta devuelve {@code null}; la capa de servicio
     * debe decidir cómo presentar ese caso al usuario (típicamente
     * mostrando {@code "—"} o {@code "0 %"}).</p>
     *
     * @param organizadorId identificador del organizador.
     * @return porcentaje medio de ocupación entre 0 y 100; {@code null} si
     *         el organizador no tiene eventos con aforo positivo.
     */
    @Query("SELECT AVG((e.entradasVendidas * 100.0) / e.aforoMaximo) FROM Evento e " +
           "WHERE e.organizador.id = :organizadorId AND e.aforoMaximo > 0")
    Double avgOcupacionByOrganizadorId(@Param("organizadorId") Long organizadorId);

    /**
     * Devuelve el número total de eventos creados por un organizador.
     *
     * <p>Equivale a {@code findByOrganizadorId(...).size()} pero se ejecuta
     * como una sola consulta {@code COUNT(*)} a nivel de base de datos,
     * sin materializar las entidades, por lo que resulta mucho más
     * eficiente para el cuadro de mando.</p>
     *
     * @param organizadorId identificador del organizador.
     * @return número de eventos del organizador; {@code 0L} si no tiene
     *         ninguno.
     */
    long countByOrganizadorId(Long organizadorId);
}