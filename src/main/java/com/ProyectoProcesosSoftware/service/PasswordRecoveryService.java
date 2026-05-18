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

/**
 * Servicio de aplicación encargado del flujo de <em>recuperación de
 * contraseña</em> mediante token de un solo uso (T-26).
 *
 * <p>Implementa el patrón clásico "olvidé mi contraseña" en dos pasos:</p>
 * <ol>
 *   <li>El usuario solicita la recuperación indicando su email
 *       ({@link #generarTokenRecuperacion(String)}). El servicio genera
 *       un token aleatorio (UUID v4) con caducidad de 24 horas, lo guarda
 *       asociado al usuario y lo devuelve para que el canal de
 *       notificación (típicamente correo electrónico) se lo haga llegar.</li>
 *   <li>El usuario presenta el token junto con la nueva contraseña
 *       ({@link #cambiarPassword(String, String)}). El servicio valida
 *       que el token exista y no haya expirado, actualiza la contraseña
 *       cifrada del usuario y elimina el token para impedir su
 *       reutilización (uso único).</li>
 * </ol>
 *
 * <h2>Decisiones de seguridad</h2>
 * <ul>
 *   <li><b>No revelar la existencia del email</b>: si el email no
 *       corresponde a ningún usuario, {@link #generarTokenRecuperacion(String)}
 *       devuelve {@code null} en lugar de lanzar una excepción. Así el
 *       endpoint público no permite enumerar cuentas registradas.</li>
 *   <li><b>Un único token activo por usuario</b>: antes de crear un token
 *       nuevo se eliminan los anteriores del mismo usuario, evitando que
 *       varios tokens válidos coexistan e impidiendo ataques basados en
 *       acumulación de tokens.</li>
 *   <li><b>Token aleatorio fuerte</b>: se usa {@link UUID#randomUUID()},
 *       que aporta entropía suficiente (122 bits) para hacer inviable la
 *       adivinación por fuerza bruta.</li>
 *   <li><b>Caducidad explícita</b>: 24 horas desde la generación, lo que
 *       limita la ventana de exposición ante una eventual filtración.</li>
 *   <li><b>Borrado tras el uso</b>: tras un cambio de contraseña exitoso,
 *       o tras detectar que un token está expirado, el token se elimina
 *       del repositorio para garantizar uso único.</li>
 *   <li><b>Cifrado de la nueva contraseña</b>: la contraseña se almacena
 *       cifrada mediante {@link PasswordEncoder}, nunca en claro.</li>
 * </ul>
 *
 * @author Equipo Proyecto Procesos Software
 * @see UsuarioRepository
 * @see TokenRecuperacionRepository
 * @see TokenRecuperacion
 * @see PasswordEncoder
 */
@Service
public class PasswordRecoveryService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Genera un token de recuperación de contraseña para el usuario cuyo
     * email se indica, sustituyendo cualquier token anterior que estuviera
     * vigente para esa misma cuenta.
     *
     * <p>Si el email no corresponde a ningún usuario registrado, el método
     * devuelve {@code null} <em>sin lanzar excepción</em>. Esta decisión es
     * deliberada y forma parte del diseño de seguridad: el endpoint público
     * que invoque a este método debe responder al cliente con el mismo
     * mensaje genérico tanto si el email existía como si no, evitando
     * exponer la lista de usuarios registrados ante un atacante.</p>
     *
     * <p>Cuando el usuario sí existe:</p>
     * <ol>
     *   <li>Se eliminan todos los tokens previos asociados a su id (un
     *       único token activo por usuario).</li>
     *   <li>Se crea un nuevo {@link TokenRecuperacion} con un identificador
     *       aleatorio basado en {@link UUID#randomUUID()} y fecha de
     *       expiración fijada en {@code ahora + 24 horas}.</li>
     *   <li>Se persiste el token y se devuelve su valor en cadena para que
     *       el canal de notificación lo entregue al usuario (típicamente
     *       como parte de una URL).</li>
     * </ol>
     *
     * @param email dirección de correo del usuario que solicita recuperar
     *              su contraseña. No puede ser {@code null}.
     * @return la cadena del token recién generado si el usuario existe;
     *         {@code null} si no hay ningún usuario registrado con ese
     *         email.
     */
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

    /**
     * Cambia la contraseña del usuario asociado a un token de recuperación
     * previamente emitido, validando que el token sea válido y no haya
     * expirado, y eliminándolo tras su uso.
     *
     * <p>El flujo realiza, en este orden:</p>
     * <ol>
     *   <li>Localiza el token en el repositorio. Si no existe, lanza
     *       {@link BusinessRuleException} con mensaje
     *       <em>"Token inválido"</em>.</li>
     *   <li>Comprueba la caducidad mediante
     *       {@link TokenRecuperacion#isExpirado()}. Si el token está
     *       expirado, lo borra del repositorio (no debe seguir ocupando
     *       espacio ni dar lugar a reusos accidentales) y lanza
     *       {@link BusinessRuleException} con mensaje
     *       <em>"Token expirado"</em>.</li>
     *   <li>Cifra la nueva contraseña con {@link PasswordEncoder#encode(CharSequence)}
     *       y la asigna al usuario asociado al token.</li>
     *   <li>Persiste al usuario y borra el token, garantizando así el
     *       <strong>uso único</strong>: un mismo enlace de recuperación
     *       solo puede consumirse una vez.</li>
     * </ol>
     *
     * @param tokenStr        valor del token de recuperación que el usuario
     *                        ha recibido (típicamente extraído de la URL del
     *                        enlace que se le envió por correo).
     * @param nuevaPassword   nueva contraseña en claro. Será cifrada antes
     *                        de almacenarse; nunca se persiste en claro.
     * @throws BusinessRuleException si el token no existe ({@code "Token inválido"})
     *                               o si ha caducado ({@code "Token expirado"}).
     */
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