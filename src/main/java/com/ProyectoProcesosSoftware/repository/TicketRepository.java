package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Método existente 
    List<Ticket> findByAsistenteId(Long asistenteId);

    // Método nuevo para getMisEntradas ordenado
    List<Ticket> findByAsistenteIdOrderByFechaCompraDesc(Long asistenteId);

    List<Ticket> findByEventoId(Long eventoId);

    boolean existsByEventoIdAndAsistenteId(Long eventoId, Long asistenteId);
}