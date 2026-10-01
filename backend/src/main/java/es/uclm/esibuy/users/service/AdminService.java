package es.uclm.esibuy.users.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.CandidatoDTO;
import es.uclm.esibuy.users.dto.PromoverAdminDTO;
import es.uclm.esibuy.users.dto.UserAdminDTO;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private static final String DEFAULT_ADMIN_AVATAR = "avatar-admin-default";

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<CandidatoDTO> obtenerCandidatos() {
        return userRepository.findByRoleIn(List.of(UserRole.CLIENT, UserRole.SELLER))
                .stream()
                .map(CandidatoDTO::fromUser)
                .toList();
    }

    public User promoverAAdmin(String idUsuario, PromoverAdminDTO request) {
        User user = userRepository.findById(idUsuario)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalStateException("El usuario ya tiene rol de administrador");
        }

        user.setFirstName(request.getNombre().trim());
        user.setLastName(request.getApellidos().trim());

        String profileImage = (request.getImagenPerfil() == null || request.getImagenPerfil().isBlank())
                ? DEFAULT_ADMIN_AVATAR
                : request.getImagenPerfil().trim();
        user.setProfileImage(profileImage);

        LocalDateTime hireDate = user.getCreatedAt() != null
                ? LocalDateTime.ofInstant(user.getCreatedAt(), ZoneOffset.UTC)
                : LocalDateTime.now();

        AdminProfile adminProfile = new AdminProfile(blankToNull(request.getSede()), hireDate);
        user.setAdminProfile(adminProfile);

        user.setRole(UserRole.ADMIN);
        user.setActive(true);

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
