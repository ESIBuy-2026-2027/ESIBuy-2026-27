package es.uclm.esibuy.users.service;

import org.springframework.stereotype.Service;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.dto.UserResponseDTO;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public AdminUserService(UserRepository userRepository, ProductRepository productRepository,
            UserService userService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    public User obtenerPorId(String id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    public User actualizar(String id, UpdateUserRequestDTO request) {
        User user = obtenerPorId(id);
        actualizarDatosGenerales(user, request);

        switch (user.getRole()) {
            case ADMIN -> actualizarPerfilAdmin(user, request);
            case SELLER -> actualizarPerfilSeller(user, request);
            case CLIENT -> actualizarPerfilCliente(user, request);
        }

        return userRepository.save(user);
    }

    private void actualizarDatosGenerales(User user, UpdateUserRequestDTO request) {
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setNationalId(blankToNull(request.getNationalId()));
        user.setProfileImage(blankToNull(request.getProfileImage()));

        if (user.getRole() != UserRole.ADMIN) {
            user.setPhone(blankToNull(request.getPhone()));
        }
    }

    private void actualizarPerfilAdmin(User user, UpdateUserRequestDTO request) {
        if (request.getHeadquarters() == null) {
            return;
        }

        AdminProfile profile = user.getAdminProfile();
        if (profile == null) {
            profile = new AdminProfile();
            user.setAdminProfile(profile);
        }
        profile.setHeadquarters(blankToNull(request.getHeadquarters()));
    }

    private void actualizarPerfilSeller(User user, UpdateUserRequestDTO request) {
        SellerProfile profile = user.getSellerProfile();
        if (profile == null) {
            profile = new SellerProfile();
            user.setSellerProfile(profile);
        }

        if (request.getBusinessName() != null) {
            String newBusinessName = blankToNull(request.getBusinessName());
            if (newBusinessName != null && !newBusinessName.equalsIgnoreCase(profile.getBusinessName())
                    && userRepository.existsBySellerProfileBusinessNameIgnoreCase(newBusinessName)) {
                throw new DuplicateUserException("Ya existe un vendedor con ese nombre comercial");
            }
            profile.setBusinessName(newBusinessName);
        }

        if (request.getDefaultCategory() != null) {
            profile.setDefaultCategory(blankToNull(request.getDefaultCategory()));
        }
    }

    private void actualizarPerfilCliente(User user, UpdateUserRequestDTO request) {
        if (request.getBirthDate() == null) {
            return;
        }

        ClientProfile profile = user.getClientProfile();
        if (profile == null) {
            profile = new ClientProfile();
            user.setClientProfile(profile);
        }
        profile.setBirthDate(request.getBirthDate());
    }

    public void eliminar(String id, String authenticatedUserId) {
        if (id == null || !id.matches("[a-fA-F0-9]{24}")) {
            throw new IllegalStateException("El identificador del usuario no es válido");
        }
        if (id.equalsIgnoreCase(authenticatedUserId)) {
            throw new IllegalStateException("No puedes eliminar tu propia cuenta");
        }

        User user = obtenerPorId(id);
        if (user.getRole() == UserRole.SELLER && productRepository.countBySellerId(id) > 0) {
            throw new UserDeletionConflictException();
        }

        userRepository.delete(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public UserResponseDTO createClientAdmin(ClientRegistrationRequest request) {
        User createdUser = userService.registerClient(request, true);
        return mapToDTO(createdUser);
    }

    public UserResponseDTO createSellerAdmin(SellerRegistrationRequest request) {
        User createdUser = userService.registerSeller(request, true);
        return mapToDTO(createdUser);
    }

    private UserResponseDTO mapToDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name());
    }
}