package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


/**
 * Repositorio Spring Data JPA para la entidad {@link Usuario}.
 *
 * <p>Al extender {@link JpaRepository} hereda automáticamente las
 * operaciones CRUD básicas. Esta interfaz añade dos métodos derivados por
 * nombre centrados en el campo {@code email}, que actúa como
 * identificador funcional único de las cuentas y se utiliza tanto en el
 * registro (para garantizar unicidad) como en la autenticación y la
 * recuperación de contraseña (para localizar al usuario).</p>
 *
 * @author Equipo Proyecto Procesos Software
 * @see Usuario
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Indica si ya existe un usuario registrado con el email indicado.
     *
     * <p>Lo utiliza {@code UsuarioService} antes de crear una nueva cuenta
     * o de aceptar un cambio de email en un perfil existente, con el fin
     * de garantizar la unicidad del correo electrónico en la plataforma.</p>
     *
     * @param email dirección de correo a comprobar.
     * @return {@code true} si ya existe un usuario con ese email;
     *         {@code false} en caso contrario.
     */
    boolean existsByEmail(String email);

    /**
     * Busca un usuario a partir de su email.
     *
     * <p>Se utiliza, entre otros, en el flujo de autenticación y en la
     * generación de tokens de recuperación de contraseña
     * ({@code PasswordRecoveryService}), donde el email es el dato que
     * aporta el usuario para identificarse.</p>
     *
     * @param email dirección de correo del usuario a localizar.
     * @return un {@link Optional} con el usuario si existe; vacío si no
     *         hay ninguno con ese email.
     */
    Optional<Usuario> findByEmail(String email);
}