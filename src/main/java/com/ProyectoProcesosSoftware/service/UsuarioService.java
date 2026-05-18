package com.ProyectoProcesosSoftware.service;

import com.ProyectoProcesosSoftware.dto.EditarUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.RegistroUsuarioDTO;
import com.ProyectoProcesosSoftware.dto.UsuarioMapper;
import com.ProyectoProcesosSoftware.dto.UsuarioResponseDTO;
import com.ProyectoProcesosSoftware.exception.DuplicateResourceException;
import com.ProyectoProcesosSoftware.exception.ResourceNotFoundException;
import com.ProyectoProcesosSoftware.exception.UnauthorizedActionException;
import com.ProyectoProcesosSoftware.model.Rol;
import com.ProyectoProcesosSoftware.model.Usuario;
import com.ProyectoProcesosSoftware.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación encargado de la gestión integral de las cuentas
 * de usuario de la plataforma.
 *
 * <p>Esta clase concentra la lógica de negocio relacionada con el alta,
 * consulta, edición y baja de usuarios, tanto asistentes como organizadores.
 * Actúa como capa intermedia entre los controladores REST y el repositorio
 * de persistencia, garantizando:</p>
 *
 * <ul>
 *   <li>El <em>cifrado seguro</em> de las contraseñas en el momento del alta
 *       mediante el {@link PasswordEncoder} configurado por Spring Security
 *       (típicamente BCrypt). Las contraseñas nunca se almacenan en claro.</li>
 *   <li>La <em>unicidad del email</em>: no se permite registrar dos cuentas
 *       con el mismo correo, ni cambiar el email de una cuenta existente a
 *       uno ya ocupado por otra.</li>
 *   <li>El <em>control de autorización</em>: un usuario solo puede consultar,
 *       editar o eliminar <strong>su propia</strong> cuenta. Cualquier
 *       intento de operar sobre la cuenta de otro usuario se rechaza con
 *       {@link UnauthorizedActionException}.</li>
 *   <li>La <em>traducción</em> entre la entidad de dominio {@link Usuario} y
 *       los DTOs expuestos a la capa de presentación, evitando la fuga de
 *       campos sensibles como el hash de la contraseña.</li>
 *   <li>La <em>aplicación transaccional</em> de los cambios sobre la base
 *       de datos mediante {@link Transactional}.</li>
 * </ul>
 *
 * @author Equipo Proyecto Procesos Software
 * @see UsuarioRepository
 * @see UsuarioMapper
 * @see PasswordEncoder
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario en la plataforma a partir de los datos
     * recibidos, cifrando la contraseña antes de persistir.
     *
     * <p>Antes de crear la cuenta se verifica que no exista ya otro usuario
     * con el mismo email; en caso afirmativo se lanza
     * {@link DuplicateResourceException} y no se realiza ninguna escritura.
     * Si la validación de unicidad se supera, se construye una nueva entidad
     * {@link Usuario} con:</p>
     * <ul>
     *   <li>nombre y email tal cual se reciben en el DTO,</li>
     *   <li>contraseña <em>cifrada</em> mediante {@link PasswordEncoder#encode(CharSequence)},
     *       de modo que la versión en claro nunca llega a almacenarse,</li>
     *   <li>rol convertido a mayúsculas y mapeado al valor correspondiente
     *       del enumerado {@link Rol}.</li>
     * </ul>
     *
     * @param dto objeto con los datos del nuevo usuario: nombre, email,
     *            contraseña en claro y rol como cadena ("ASISTENTE" /
     *            "ORGANIZADOR", admite cualquier capitalización). No puede
     *            ser {@code null}.
     * @return un {@link UsuarioResponseDTO} con la información pública del
     *         usuario recién creado (incluyendo su identificador asignado).
     *         La contraseña nunca se devuelve.
     * @throws DuplicateResourceException si ya existe un usuario registrado
     *                                    con el mismo email.
     * @throws IllegalArgumentException   si el valor de rol recibido no se
     *                                    corresponde con ningún elemento del
     *                                    enumerado {@link Rol}.
     */
    @Transactional
    public UsuarioResponseDTO registrar(RegistroUsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + dto.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(Rol.valueOf(dto.getRol().toUpperCase()));

        Usuario guardado = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponseDTO(guardado);
    }

    /**
     * Obtiene los datos públicos del perfil de un usuario, garantizando que
     * solo el propio interesado pueda consultarlo.
     *
     * <p>Antes de acceder al repositorio se comprueba que el identificador
     * solicitado coincide con el del usuario autenticado en la petición; en
     * caso contrario se rechaza la operación con
     * {@link UnauthorizedActionException}, sin revelar siquiera si la cuenta
     * objetivo existe o no.</p>
     *
     * @param id                    identificador del perfil que se desea
     *                              consultar. No puede ser {@code null}.
     * @param usuarioAutenticadoId  identificador del usuario que invoca la
     *                              operación, obtenido del contexto de
     *                              seguridad. No puede ser {@code null}.
     * @return un {@link UsuarioResponseDTO} con los datos públicos del
     *         perfil solicitado (nombre, email, rol, identificador).
     * @throws UnauthorizedActionException si {@code id} no coincide con
     *                                     {@code usuarioAutenticadoId}, es
     *                                     decir, si se intenta consultar el
     *                                     perfil de otro usuario.
     * @throws ResourceNotFoundException   si no existe ningún usuario con el
     *                                     identificador indicado.
     */
    public UsuarioResponseDTO obtenerPerfil(Long id, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new UnauthorizedActionException("No puedes acceder al perfil de otro usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        return UsuarioMapper.toResponseDTO(usuario);
    }

    /**
     * Modifica el nombre y/o el email de un usuario existente, validando la
     * autoría y preservando la unicidad del correo.
     *
     * <p>El flujo realiza, en este orden:</p>
     * <ol>
     *   <li>Comprueba que el usuario autenticado coincide con el dueño del
     *       perfil; si no, lanza {@link UnauthorizedActionException}.</li>
     *   <li>Carga la entidad {@link Usuario} desde el repositorio; si no
     *       existe, lanza {@link ResourceNotFoundException}.</li>
     *   <li>Si el email del DTO difiere del actual, comprueba que ese nuevo
     *       email no esté ya en uso por <em>otro</em> usuario. En caso
     *       contrario lanza {@link DuplicateResourceException}.</li>
     *   <li>Actualiza los campos editables (nombre y email) y persiste los
     *       cambios.</li>
     * </ol>
     *
     * <p>Este método no modifica la contraseña ni el rol del usuario; esas
     * operaciones se cubren con flujos específicos.</p>
     *
     * @param id                    identificador del perfil a editar. No
     *                              puede ser {@code null}.
     * @param dto                   DTO con los nuevos valores de nombre y
     *                              email. No puede ser {@code null}.
     * @param usuarioAutenticadoId  identificador del usuario que invoca la
     *                              operación, obtenido del contexto de
     *                              seguridad. No puede ser {@code null}.
     * @return un {@link UsuarioResponseDTO} con el estado del perfil tras la
     *         actualización.
     * @throws UnauthorizedActionException si {@code id} no coincide con
     *                                     {@code usuarioAutenticadoId}.
     * @throws ResourceNotFoundException   si no existe ningún usuario con el
     *                                     identificador indicado.
     * @throws DuplicateResourceException  si el nuevo email ya está siendo
     *                                     utilizado por otro usuario distinto.
     */
    @Transactional
    public UsuarioResponseDTO editarPerfil(Long id, EditarUsuarioDTO dto, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new UnauthorizedActionException("No puedes editar el perfil de otro usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!usuario.getEmail().equals(dto.getEmail()) && usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + dto.getEmail());
        }

        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        Usuario actualizado = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponseDTO(actualizado);
    }

    /**
     * Elimina permanentemente la cuenta de un usuario, garantizando que solo
     * el propio interesado pueda solicitarla.
     *
     * <p>Antes de borrar se verifica que el usuario autenticado coincide con
     * el dueño de la cuenta indicada y que dicha cuenta existe. Si ambas
     * comprobaciones se superan, se invoca al repositorio para eliminar la
     * entidad de forma transaccional.</p>
     *
     * <p><b>Nota de integridad referencial.</b> El borrado afecta únicamente
     * a la fila de {@link Usuario}; las entidades relacionadas (eventos
     * organizados o entradas compradas) se gestionarán conforme a las
     * estrategias de cascada/limpieza definidas a nivel de modelo y a
     * eventuales requisitos legales de retención. Cualquier cambio en esas
     * políticas se debería reflejar también en esta documentación.</p>
     *
     * @param id                    identificador de la cuenta a eliminar.
     *                              No puede ser {@code null}.
     * @param usuarioAutenticadoId  identificador del usuario que invoca la
     *                              operación, obtenido del contexto de
     *                              seguridad. No puede ser {@code null}.
     * @throws UnauthorizedActionException si {@code id} no coincide con
     *                                     {@code usuarioAutenticadoId}, es
     *                                     decir, si se intenta eliminar la
     *                                     cuenta de otro usuario.
     * @throws ResourceNotFoundException   si no existe ningún usuario con el
     *                                     identificador indicado.
     */
    @Transactional
    public void eliminarCuenta(Long id, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new UnauthorizedActionException("No puedes eliminar la cuenta de otro usuario");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        usuarioRepository.delete(usuario);
    }
}