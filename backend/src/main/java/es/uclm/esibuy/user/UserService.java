package es.uclm.esibuy.user;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException("Ya existe un usuario con ese email");
        }
        if (request.getRole() == UserRole.SELLER
                && userRepository.existsByBusinessNameIgnoreCase(request.getBusinessName().trim())) {
            throw new DuplicateUserException("Ya existe un vendedor con ese nombre comercial");
        }

        User user = new User(email, request.getFirstName().trim(), request.getLastName().trim(), request.getRole());
        user.setBirthDate(request.getBirthDate());
        user.setNationalId(request.getNationalId().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProfileImage(request.getProfileImage() == null || request.getProfileImage().isBlank()
                ? defaultProfileImage(request.getRole())
                : request.getProfileImage().trim());
        user.setCustomerType(request.getRole() == UserRole.CLIENT ? request.getCustomerType() : null);
        user.setDefaultCategory(blankToNull(request.getDefaultCategory()));
        user.setBusinessName(blankToNull(request.getBusinessName()));
        return userRepository.save(user);
    }

    private String defaultProfileImage(UserRole role) {
        return role == UserRole.SELLER ? "avatar-seller-default" : "avatar-client-default";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
