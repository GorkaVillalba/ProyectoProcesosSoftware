package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link TokenRecuperacion}.
 *
 * <p>Soporta el flujo de recuperación de contraseña gestionado por
 * {@code PasswordRecoveryService}: emisión de un token aleatorio,
 * localización del token por su valor cuando el usuario hace clic en el
 * enlace recibido, y borrado del token tras su uso o expiración para
 * garantizar la política de <em>uso único</em>.</p>
 *
 * <p>Al extender {@link JpaRepository} hereda las operaciones CRUD básicas;
 * esta interfaz añade dos métodos derivados por nombre específicos del
 * dominio.</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see TokenRecuperacion
 */
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    /**
     * Localiza un token de recuperación a partir de su valor en cadena.
     *
     * <p>Lo utiliza {@code PasswordRecoveryService#cambiarPassword(String, String)}
     * para validar el token que el usuario ha presentado (típicamente
     * extraído del enlace de recuperación enviado por email).</p>
     *
     * @param token valor del token tal y como se generó originalmente
     *              (cadena UUID).
     * @return un {@link Optional} con el token si existe; vacío si el valor
     *         no corresponde a ningún token emitido.
     */
    Optional<TokenRecuperacion> findByToken(String token);

    /**
     * Elimina todos los tokens de recuperación asociados a un usuario.
     *
     * <p>Se invoca antes de generar un token nuevo para garantizar la
     * regla de <strong>un único token activo por usuario</strong>: si el
     * mismo usuario solicita recuperar su contraseña varias veces, solo
     * el último enlace emitido permanece válido, lo que evita
     * acumulaciones de tokens vigentes y reduce la superficie de ataque.</p>
     *
     * @param usuarioId identificador del usuario cuyos tokens se desean
     *                  eliminar.
     */
    void deleteByUsuarioId(Long usuarioId);
}