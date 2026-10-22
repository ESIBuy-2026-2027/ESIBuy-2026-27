package es.uclm.esibuy.users.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
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
    private static final String MSG_DUPLICATE_EMAIL = "No se ha podido completar el registro. Comprueba los datos introducidos e inténtalo de nuevo";
    private static final String MSG_USER_NOT_FOUND = "Usuario no encontrado";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createAdmin(AltaAdminDTO request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(MSG_DUPLICATE_EMAIL);
        }

        User user = new User(email, request.getFirstName().trim(), request.getLastName().trim(), UserRole.ADMIN);
        user.setNationalId(request.getNationalId().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        String profileImage = (request.getProfileImage() == null || request.getProfileImage().isBlank())
                ? DEFAULT_ADMIN_AVATAR
                : request.getProfileImage().trim();
        user.setProfileImage(profileImage);

        AdminProfile adminProfile = new AdminProfile(blankToNull(request.getHeadquarters()), LocalDateTime.now());
        user.setAdminProfile(adminProfile);
        user.setActive(true);
        user.setRole(UserRole.ADMIN);

        return userRepository.save(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public List<UserAdminDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserAdminDTO::fromUser)
                .toList();
    }

    public List<UserAdminDTO> getFilteredUsers(String rol, String estado) {
        return userRepository.findAll().stream()
                .filter(u -> matchesRoleFilter(u, rol))
                .filter(u -> matchesStatusFilter(u, estado))
                .map(UserAdminDTO::fromUser)
                .toList();
    }

    private boolean matchesRoleFilter(User user, String rol) {
        if (rol == null || rol.isBlank()) {
            return true;
        }
        if (user.getRole() == null) {
            return false;
        }

        String roleName = user.getRole().name();
        String rolNormalizado = rol.trim();

        return switch (rolNormalizado.toUpperCase(Locale.ROOT)) {
            case "CLIENTE" -> "CLIENT".equals(roleName);
            case "VENDEDOR" -> "SELLER".equals(roleName);
            case "ADMIN" -> "ADMIN".equals(roleName);
            default -> roleName.equalsIgnoreCase(rolNormalizado);
        };
    }

    private boolean matchesStatusFilter(User user, String estado) {
        if (estado == null || estado.isBlank()) {
            return true;
        }

        return switch (estado.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVO" -> user.isActive() && !user.isBlocked();
            case "INACTIVO" -> !user.isActive() && !user.isBlocked();
            case "BLOQUEADO" -> user.isBlocked();
            default -> false;
        };
    }

    public void activateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND));

        // Una cuenta bloqueada no puede quedar activa
        if (!user.isBlocked()) {
            user.setActive(true);
            userRepository.save(user);
        }
    }

    public void deactivateUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND));

        user.setActive(false);
        userRepository.save(user);
    }

    public void blockUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND));

        // Bloquear implica también desactivar la cuenta
        user.setActive(false);
        user.setBlocked(true);
        userRepository.save(user);
    }

    public void unblockUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND));

        user.setBlocked(false);
        user.setActive(true);
        userRepository.save(user);
    }
}
