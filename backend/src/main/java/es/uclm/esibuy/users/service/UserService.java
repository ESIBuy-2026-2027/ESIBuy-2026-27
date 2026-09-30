package es.uclm.esibuy.users.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.CandidatoDTO;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.PromoverAdminDTO;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final String MSG_DUPLICATE_EMAIL = "Ya existe un usuario con ese email";
    private static final String DEFAULT_ADMIN_AVATAR = "avatar-admin-default";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        AdminProfile adminProfile = new AdminProfile(blankToNull(request.getSede()), LocalDateTime.now());
        user.setAdminProfile(adminProfile);

        user.setRole(UserRole.ADMIN);
        user.setActive(true);

        return userRepository.save(user);
    }

    public User registerClient(ClientRegistrationRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(MSG_DUPLICATE_EMAIL);
        }

        User user = new User(email, request.getFirstName().trim(), request.getLastName().trim(), UserRole.CLIENT);
        user.setNationalId(request.getNationalId().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProfileImage(request.getProfileImage() == null || request.getProfileImage().isBlank()
                ? "avatar-client-default"
                : request.getProfileImage().trim());

        ClientProfile clientProfile = new ClientProfile(request.getBirthDate(), request.getCustomerType());
        user.setClientProfile(clientProfile);

        return userRepository.save(user);
    }

    public User registerSeller(SellerRegistrationRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(MSG_DUPLICATE_EMAIL);
        }
        String businessName = request.getBusinessName().trim();
        if (userRepository.existsBySellerProfileBusinessNameIgnoreCase(businessName)) {
            throw new DuplicateUserException("Ya existe un vendedor con ese nombre comercial");
        }

        User user = new User(email, request.getFirstName().trim(), request.getLastName().trim(), UserRole.SELLER);
        user.setNationalId(request.getNationalId().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProfileImage(request.getProfileImage() == null || request.getProfileImage().isBlank()
                ? "avatar-seller-default"
                : request.getProfileImage().trim());

        SellerProfile sellerProfile = new SellerProfile(businessName, request.getDefaultCategory().trim());
        user.setSellerProfile(sellerProfile);

        return userRepository.save(user);
    }

    public User createAdmin(AdminCreateRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException(MSG_DUPLICATE_EMAIL);
        }

        User user = new User(email, request.getFirstName().trim(), request.getLastName().trim(), UserRole.ADMIN);
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProfileImage(request.getProfileImage() == null || request.getProfileImage().isBlank()
                ? DEFAULT_ADMIN_AVATAR
                : request.getProfileImage().trim());

        AdminProfile adminProfile = new AdminProfile(blankToNull(request.getHeadquarters()));
        user.setAdminProfile(adminProfile);

        return userRepository.save(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
