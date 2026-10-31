package es.uclm.esibuy.users.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.uclm.esibuy.auth.passwordreset.repository.PasswordResetTokenRepository;
import es.uclm.esibuy.cart.model.Cart;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.UpdateOwnProfileRequestDTO;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.wishlist.repository.WishlistRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.security.repository.EmailOtpChallengeRepository;
import es.uclm.esibuy.security.repository.LoginAttemptRepository;

@Service
public class UserService {

    private static final String MSG_DUPLICATE_EMAIL = "No se ha podido completar el registro. Comprueba los datos introducidos e inténtalo de nuevo";
    private static final String DELETED_CUSTOMER_EMAIL = "cliente.eliminado@esibuy.local";

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final EmailOtpChallengeRepository emailOtpChallengeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CartRepository cartRepository,
            WishlistRepository wishlistRepository,
            EmailOtpChallengeRepository emailOtpChallengeRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            LoginAttemptRepository loginAttemptRepository) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.wishlistRepository = wishlistRepository;
        this.emailOtpChallengeRepository = emailOtpChallengeRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
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

    public void deleteOwnAccount(String userId) {
        User user = findById(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new UserDeletionConflictException(
                    "No puedes eliminar tu propia cuenta de administrador. "
                            + "Solicita a otro administrador que la elimine.");
        }

        if (user.getRole() == UserRole.SELLER) {
            deleteSellerAccount(user);
        } else if (user.getRole() == UserRole.CLIENT) {
            deleteClientAccount(user);
        }

        cleanSecurityData(user);
        userRepository.delete(user);
    }

    private void deleteClientAccount(User user) {
        User deletedCustomer = getOrCreateDeletedCustomer();

        List<Order> orders = orderRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(user.getId());

        for (Order order : orders) {
            order.setCustomerId(deletedCustomer.getId());
            order.anonymizeCustomerData();
            orderRepository.save(order);
        }

        cartRepository.deleteByCustomerId(user.getId());
        wishlistRepository.deleteAllByUserId(user.getId());
    }

    private User getOrCreateDeletedCustomer() {
        return userRepository.findByEmailIgnoreCase(DELETED_CUSTOMER_EMAIL)
                .orElseGet(() -> {
                    User deletedCustomer = new User(
                            DELETED_CUSTOMER_EMAIL,
                            "Cliente",
                            "eliminado",
                            UserRole.CLIENT);

                    deletedCustomer.setActive(false);
                    deletedCustomer.setBlocked(true);
                    deletedCustomer.setPasswordHash(null);

                    return userRepository.save(deletedCustomer);
                });
    }

    private void deleteSellerAccount(User seller) {
        List<Product> products = productRepository.findAllBySellerId(seller.getId());

        List<String> productIds = products.stream()
                .map(Product::getId)
                .toList();

        if (!productIds.isEmpty()) {
            List<Order> blockingOrders = orderRepository
                    .findAllByItemsProductIdInAndStatusIn(
                            productIds,
                            List.of(OrderStatus.PENDIENTE, OrderStatus.PREPARANDO));

            if (!blockingOrders.isEmpty()) {
                throw new UserDeletionConflictException(
                        "No puedes eliminar la cuenta porque tienes "
                                + blockingOrders.size()
                                + " pedidos pendientes o en preparación. "
                                + "Gestiona esos pedidos antes de solicitar la baja.");
            }

            // Conservar la relación histórica entre los pedidos y el vendedor.
            List<Order> sellerOrders = orderRepository
                    .findAllByItemsProductIdInAndStatusIn(
                            productIds,
                            List.of(
                                    OrderStatus.PAGADO,
                                    OrderStatus.ENVIADO,
                                    OrderStatus.ENTREGADO,
                                    OrderStatus.CANCELADO));

            for (Order order : sellerOrders) {
                order.assignSellerToItems(seller.getId(), productIds);
                orderRepository.save(order);
            }

            // Eliminar referencias a productos que van a desaparecer.
            cleanProductReferences(productIds);

            productRepository.deleteAll(products);
        }

        cartRepository.deleteByCustomerId(seller.getId());
        wishlistRepository.deleteAllByUserId(seller.getId());
    }

    private void cleanProductReferences(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        for (Cart cart : cartRepository.findAll()) {
            boolean modified = false;

            for (String productId : productIds) {
                modified |= cart.removeItem(productId);
            }

            if (modified) {
                cartRepository.save(cart);
            }
        }

        wishlistRepository.deleteAllByProductIdIn(productIds);
    }

    private void cleanSecurityData(User user) {
        emailOtpChallengeRepository.deleteAllByUserId(user.getId());
        passwordResetTokenRepository.deleteAllByUserId(user.getId());
        loginAttemptRepository.deleteAllByEmail(user.getEmail());
    }

    public UserProfileResponse updateOwnProfile(
            String userId,
            UpdateOwnProfileRequestDTO request) {

        User user = findById(userId);
        updateCommonProfileData(user, request);
        updateSellerProfile(user, request);
        updateClientProfile(user, request);
        updateAdminProfile(user, request);

        User savedUser = userRepository.save(user);
        return UserProfileResponse.from(savedUser);
    }

    private void updateCommonProfileData(User user,
            UpdateOwnProfileRequestDTO request) {

        // Datos generales que pueden modificar cliente y vendedor.
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(blankToNull(request.getPhone()));
        user.setProfileImage(blankToNull(request.getProfileImage()));

        // Cambiar el DNI solo si el usuario ha introducido uno nuevo.
        // Nunca utilizar el DNI enmascarado recibido en la consulta.
        if (request.getNationalId() != null) {
            user.setNationalId(request.getNationalId());
        }
    }

    private void updateSellerProfile(User user,
            UpdateOwnProfileRequestDTO request) {
        // Datos exclusivos del vendedor.
        if (user.getRole() != UserRole.SELLER) {
            return;
        }

        SellerProfile profile = user.getSellerProfile();
        if (profile == null) {
            profile = new SellerProfile();
            user.setSellerProfile(profile);
        }

        if (request.getBusinessName() != null) {
            String businessName = blankToNull(request.getBusinessName());

            if (businessName == null) {
                throw new IllegalArgumentException(
                        "El nombre comercial no puede estar vacío");
            }

            boolean sameBusinessName = profile.getBusinessName() != null
                    && businessName.equalsIgnoreCase(profile.getBusinessName());

            if (!sameBusinessName
                    && userRepository.existsBySellerProfileBusinessNameIgnoreCase(
                            businessName)) {
                throw new DuplicateUserException(
                        "Ya existe un vendedor con ese nombre comercial");
            }

            profile.setBusinessName(businessName);
        }

        if (request.getDefaultCategory() != null) {
            profile.setDefaultCategory(blankToNull(request.getDefaultCategory()));
        }
    }

    private void updateClientProfile(User user,
            UpdateOwnProfileRequestDTO request) {
        if (user.getRole() == UserRole.CLIENT
                && request.getBirthDate() != null) {

            ClientProfile profile = user.getClientProfile();

            if (profile == null) {
                profile = new ClientProfile();
                user.setClientProfile(profile);
            }

            profile.setBirthDate(request.getBirthDate());
        }
    }

    private void updateAdminProfile(User user,
            UpdateOwnProfileRequestDTO request) {
        if (request.getHeadquarters() != null) {
            AdminProfile profile = user.getAdminProfile();

            if (profile == null) {
                profile = new AdminProfile();
            }

            String headquarters = request.getHeadquarters().trim();

            profile.setHeadquarters(
                    headquarters.isEmpty() ? null : headquarters);

            user.setAdminProfile(profile);
        }
    }

    public boolean isEmailOtpEnabled(String userId) {
        User user = findById(userId);

        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.SELLER) {
            throw new IllegalStateException(
                    "La configuración del OTP por correo solo está disponible "
                            + "para administradores y vendedores");
        }

        return user.isEmailOtpEnabled();
    }

    public User updateEmailOtpEnabled(String userId, boolean enabled) {
        User user = findById(userId);

        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.SELLER) {
            throw new IllegalStateException(
                    "La configuración del OTP por correo solo está disponible "
                            + "para administradores y vendedores");
        }

        user.setEmailOtpEnabled(enabled);
        return userRepository.save(user);
    }

}
