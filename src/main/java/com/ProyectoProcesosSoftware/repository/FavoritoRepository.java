package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);

    @Transactional
    void deleteByUsuarioIdAndEventoId(Long usuarioId, Long eventoId);
}