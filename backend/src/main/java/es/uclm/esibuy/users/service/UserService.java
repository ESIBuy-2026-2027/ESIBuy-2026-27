package es.uclm.esibuy.users.service;

import java.util.Locale;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                ? "avatar-admin-default"
                : request.getProfileImage().trim());

        AdminProfile adminProfile = new AdminProfile(blankToNull(request.getHeadquarters()));
        user.setAdminProfile(adminProfile);

        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Buscamos al usuario. Si no existe, lanzamos el mensaje genérico aséptico.
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        // Comprobamos la contraseña. Si falla, lanzamos EXACTAMENTE el mismo mensaje.
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
