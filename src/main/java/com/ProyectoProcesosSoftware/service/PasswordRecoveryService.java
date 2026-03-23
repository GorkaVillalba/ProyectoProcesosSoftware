package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.exception.BusinessRuleException;
import com.ProyectoProcesosSoftware.model.TokenRecuperacion;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.TokenRecuperacionRepository;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// T-26 (Persona 5): Servicio de recuperación de contraseña
@Service
public class PasswordRecoveryService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public String generarTokenRecuperacion(String email) {
        var usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) return null;

        Usuario usuario = usuarioOpt.get();
        tokenRepository.deleteByUsuarioId(usuario.getId());

        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuario(usuario);
        token.setToken(UUID.randomUUID().toString());
        token.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        tokenRepository.save(token);

        return token.getToken();
    }

    @Transactional
    public void cambiarPassword(String tokenStr, String nuevaPassword) {
        TokenRecuperacion token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BusinessRuleException("Token inválido"));
        if (token.isExpirado()) {
            tokenRepository.delete(token);
            throw new BusinessRuleException("Token expirado");
        }
        Usuario usuario = token.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        tokenRepository.delete(token);
    }
}
