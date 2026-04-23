package com.ProyectoProcesosSoftware.dto;

import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.model.Ticket;
import com.ProyectoProcesosSoftware.model.Usuario;

public class TicketMapper {
        public static TicketResponseDTO TicketResponseDTO(Ticket t, String estrategia) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(t.getId());
        dto.setUuid(t.getUuid().toString());
        dto.setEventoId(t.getEvento().getId());
        dto.setEventoNombre(t.getEvento().getNombre());
        dto.setAsistenteId(t.getAsistente().getId());
        dto.setAsistenteNombre(t.getAsistente().getNombre());
        dto.setPrecioFinal(t.getPrecioFinal());
        dto.setEstrategiaPrecio(estrategia);
        dto.setFechaCompra(t.getFechaCompra());
        dto.setEstado(t.getEstado());
        return dto;
    }
}






    
