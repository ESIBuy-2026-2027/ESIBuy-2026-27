package es.uclm.esibuy.users.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.users.dto.AltaAdminDTO;
import es.uclm.esibuy.users.dto.UserAdminDTO;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private static final String DEFAULT_ADMIN_AVATAR = "avatar-admin-default";
    private static final String MSG_DUPLICATE_EMAIL = "Ya existe un usuario con ese email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User crearAdministrador(AltaAdminDTO request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(MSG_DUPLICATE_EMAIL);
        }

        User user = new User(email, request.getNombre().trim(), request.getApellidos().trim(), UserRole.ADMIN);
        user.setNationalId(request.getDni().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getContrasena()));

        String profileImage = (request.getImagenPerfil() == null || request.getImagenPerfil().isBlank())
                ? DEFAULT_ADMIN_AVATAR
                : request.getImagenPerfil().trim();
        user.setProfileImage(profileImage);

        AdminProfile adminProfile = new AdminProfile(blankToNull(request.getSede()), LocalDateTime.now());
        user.setAdminProfile(adminProfile);
        user.setActive(true);
        user.setRole(UserRole.ADMIN);

        return userRepository.save(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public List<UserAdminDTO> obtenerTodosLosUsuarios() {
        return userRepository.findAll().stream()
                .map(UserAdminDTO::fromUser)
                .toList();
    }

    public List<UserAdminDTO> obtenerUsuariosFiltrados(String rol, String estado) {
        return userRepository.findAll().stream()
                .filter(u -> {
                    // Filtro de Rol
                    if (rol == null || rol.isBlank())
                        return true;
                    if (u.getRole() == null)
                        return false;

                    String roleName = u.getRole().name();
                    if (rol.equalsIgnoreCase("CLIENTE") && roleName.equals("CLIENT"))
                        return true;
                    if (rol.equalsIgnoreCase("VENDEDOR") && roleName.equals("SELLER"))
                        return true;
                    if (rol.equalsIgnoreCase("ADMIN") && roleName.equals("ADMIN"))
                        return true;

                    return roleName.equalsIgnoreCase(rol);
                })
                .filter(u -> {
                    // Filtro de Estado
                    if (estado == null || estado.isBlank())
                        return true;

                    boolean isActivo = estado.equalsIgnoreCase("activo");
                    return u.isActive() == isActivo;
                })
                .map(UserAdminDTO::fromUser)
                .toList();
    }
}
