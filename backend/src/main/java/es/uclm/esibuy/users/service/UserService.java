package es.uclm.esibuy.users.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class UserService {

    private static final String MSG_DUPLICATE_EMAIL = "No se ha podido completar el registro. Comprueba los datos introducidos e inténtalo de nuevo";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerClient(ClientRegistrationRequest request) {
        return registerClient(request, false); // Llama al método de abajo como 'false'
    }

    public User registerClient(ClientRegistrationRequest request, boolean isAutoActive) {
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

        user.setActive(isAutoActive);

        ClientProfile clientProfile = new ClientProfile(request.getBirthDate(), request.getCustomerType());
        user.setClientProfile(clientProfile);

        return userRepository.save(user);
    }

    public User registerSeller(SellerRegistrationRequest request) {
        return registerSeller(request, false);
    }

    public User registerSeller(SellerRegistrationRequest request, boolean isAutoActive) {
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

        user.setActive(isAutoActive);

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
        return userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.isActive()
                        && !user.isBlocked()
                        && user.getPasswordHash() != null
                        && passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public UserProfileResponse getOwnProfile(String userId) {
        return UserProfileResponse.from(findById(userId));
    }
}
