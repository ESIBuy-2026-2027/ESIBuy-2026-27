package es.uclm.esibuy.users.service;

import java.time.LocalDate;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.wishlist.repository.WishlistRepository;
import es.uclm.esibuy.security.repository.EmailOtpChallengeRepository;
import es.uclm.esibuy.auth.passwordreset.repository.PasswordResetTokenRepository;
import es.uclm.esibuy.security.repository.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.products.model.Product;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private EmailOtpChallengeRepository emailOtpChallengeRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, passwordEncoder, productRepository,
                orderRepository, cartRepository, wishlistRepository,
                emailOtpChallengeRepository, passwordResetTokenRepository,
                loginAttemptRepository);
    }

    @Test
    void shouldRegisterClientSuccessfully() {
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setFirstName("Ana");
        request.setLastName("García");
        request.setEmail("ana@example.com");
        request.setNationalId("12345678Z");
        request.setBirthDate(LocalDate.now().minusYears(20));
        request.setPassword("Password123!");
        request.setRepeatedPassword("Password123!");
        request.setCustomerType(CustomerType.PREMIUM);

        when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerClient(request);

        assertNotNull(result);
        assertEquals("ana@example.com", result.getEmail());
        assertEquals(UserRole.CLIENT, result.getRole());
        assertFalse(result.isActive()); // Clientes desactivados por defecto
        assertNotNull(result.getClientProfile());
        assertEquals(CustomerType.PREMIUM, result.getClientProfile().getCustomerType());
        assertEquals("hashed-pwd", result.getPasswordHash());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Ana", saved.getFirstName());
    }

    @Test
    void shouldRegisterClientWithAutoActiveTrueAndDefaultAvatar() {
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setFirstName("Ana");
        request.setLastName("García");
        request.setEmail("ana.auto@example.com");
        request.setNationalId("12345678Z");
        request.setBirthDate(LocalDate.now().minusYears(20));
        request.setPassword("Password123!");
        request.setRepeatedPassword("Password123!");
        request.setCustomerType(CustomerType.PREMIUM);
        request.setProfileImage("   ");

        when(userRepository.existsByEmailIgnoreCase("ana.auto@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerClient(request, true);

        assertTrue(result.isActive());
        assertEquals("avatar-client-default", result.getProfileImage());
    }

    @Test
    void shouldThrowDuplicateUserWhenEmailExistsOnClientRegister() {
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setEmail("ana@example.com");

        when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> userService.registerClient(request));
    }

    @Test
    void shouldRegisterSellerSuccessfully() {
        SellerRegistrationRequest request = new SellerRegistrationRequest();
        request.setFirstName("Carlos");
        request.setLastName("Lopez");
        request.setEmail("carlos@example.com");
        request.setNationalId("87654321A");
        request.setBusinessName("Carlos Tech");
        request.setDefaultCategory("Informatica");
        request.setPassword("Password123!");
        request.setRepeatedPassword("Password123!");

        when(userRepository.existsByEmailIgnoreCase("carlos@example.com")).thenReturn(false);
        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Carlos Tech")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerSeller(request);

        assertNotNull(result);
        assertEquals("carlos@example.com", result.getEmail());
        assertEquals(UserRole.SELLER, result.getRole());
        assertFalse(result.isActive()); // Vendedores desactivados por defecto
        assertNotNull(result.getSellerProfile());
        assertEquals("Carlos Tech", result.getSellerProfile().getBusinessName());
        assertEquals("Informatica", result.getSellerProfile().getDefaultCategory());
    }

    @Test
    void shouldRegisterSellerWithAutoActiveTrueAndDefaultAvatar() {
        SellerRegistrationRequest request = new SellerRegistrationRequest();
        request.setFirstName("Carlos");
        request.setLastName("López");
        request.setEmail("carlos.auto@example.com");
        request.setNationalId("87654321A");
        request.setBusinessName("Carlos Tech Autos");
        request.setDefaultCategory("Informatica");
        request.setPassword("Password123!");
        request.setRepeatedPassword("Password123!");
        request.setProfileImage("   ");

        when(userRepository.existsByEmailIgnoreCase("carlos.auto@example.com")).thenReturn(false);
        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Carlos Tech Autos")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerSeller(request, true);

        assertTrue(result.isActive());
        assertEquals("avatar-seller-default", result.getProfileImage());
    }

    @Test
    void shouldThrowDuplicateUserWhenBusinessNameExists() {
        SellerRegistrationRequest request = new SellerRegistrationRequest();
        request.setEmail("carlos@example.com");
        request.setBusinessName("Carlos Tech");

        when(userRepository.existsByEmailIgnoreCase("carlos@example.com")).thenReturn(false);
        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Carlos Tech")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> userService.registerSeller(request));
    }

    @Test
    void shouldCreateAdminSuccessfullyAndActive() {
        AdminCreateRequest request = new AdminCreateRequest();
        request.setFirstName("Admin");
        request.setLastName("Root");
        request.setEmail("admin@esibuy.com");
        request.setPassword("Admin123!Safe");
        request.setHeadquarters("Ciudad Real");

        when(userRepository.existsByEmailIgnoreCase("admin@esibuy.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin123!Safe")).thenReturn("hashed-admin-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createAdmin(request);

        assertNotNull(result);
        assertEquals("admin@esibuy.com", result.getEmail());
        assertEquals(UserRole.ADMIN, result.getRole());
        assertTrue(result.isActive()); // Admin activado por defecto
        assertNotNull(result.getAdminProfile());
        assertEquals("Ciudad Real", result.getAdminProfile().getHeadquarters());
        assertNotNull(result.getAdminProfile().getHireDate());
    }

    @Test
    void shouldCreateAdminWithBlankHeadquartersAndDefaultAvatar() {
        AdminCreateRequest request = new AdminCreateRequest();
        request.setFirstName("Admin");
        request.setLastName("Root");
        request.setEmail("admin.blank@esibuy.com");
        request.setPassword("Admin123!Safe");
        request.setRepeatedPassword("Admin123!Safe");
        request.setProfileImage("   ");
        request.setHeadquarters("   ");

        when(userRepository.existsByEmailIgnoreCase("admin.blank@esibuy.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin123!Safe")).thenReturn("hashed-admin-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createAdmin(request);

        assertEquals("avatar-admin-default", result.getProfileImage());
        assertNull(result.getAdminProfile().getHeadquarters());
    }

    @Test
    void shouldAuthenticateUserWithEmailAndPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" SELLER@example.com ");
        request.setPassword("Password123!");
        User seller = new User("seller@example.com", "Carlos", "Lopez", UserRole.SELLER);
        seller.setActive(true);
        seller.setPasswordHash("hashed-pwd");

        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(java.util.Optional.of(seller));
        when(passwordEncoder.matches("Password123!", "hashed-pwd")).thenReturn(true);

        assertEquals(seller, userService.login(request));
    }

    @Test
    void shouldRejectLoginWhenAccountIsInactive() {
        LoginRequest request = new LoginRequest();
        request.setEmail("seller@example.com");
        request.setPassword("Password123!");

        User seller = new User("seller@example.com", "Carlos", "Lopez", UserRole.SELLER);
        seller.setActive(false);
        seller.setPasswordHash("hashed-pwd");

        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(java.util.Optional.of(seller));

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldRejectLoginWhenAccountIsBlocked() {
        LoginRequest request = new LoginRequest();
        request.setEmail("seller@example.com");
        request.setPassword("Password123!");

        User seller = new User("seller@example.com", "Carlos", "Lopez", UserRole.SELLER);
        seller.setActive(true);
        seller.setBlocked(true);
        seller.setPasswordHash("hashed-pwd");

        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(java.util.Optional.of(seller));

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldRejectLoginWhenPasswordHashIsMissing() {
        LoginRequest request = new LoginRequest();
        request.setEmail("seller@example.com");
        request.setPassword("Password123!");

        User seller = new User("seller@example.com", "Carlos", "Lopez", UserRole.SELLER);
        seller.setActive(true);
        seller.setPasswordHash(null);

        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(java.util.Optional.of(seller));

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldRejectInvalidLoginCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("seller@example.com");
        request.setPassword("wrong-password");
        User seller = new User("seller@example.com", "Carlos", "Lopez", UserRole.SELLER);
        seller.setActive(true);
        seller.setPasswordHash("hashed-pwd");

        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(java.util.Optional.of(seller));
        when(passwordEncoder.matches("wrong-password", "hashed-pwd")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldListAllUsersForAdminWorkflow() {
        User admin = new User("admin@example.com", "Admin", "Root", UserRole.ADMIN);
        when(userRepository.findAll()).thenReturn(java.util.List.of(admin));

        assertEquals(java.util.List.of(admin), userService.findAllUsers());
        verify(userRepository).findAll();
    }

    @Test
    void shouldRejectLoginWhenAccountIsBlockedEvenIfActive() {
        LoginRequest request = new LoginRequest();
        request.setEmail("blocked@example.com");
        request.setPassword("Password123!");
        User user = new User("blocked@example.com", "Blocked", "User", UserRole.CLIENT);
        user.setActive(true);
        user.setBlocked(true);
        user.setPasswordHash("hash");
        when(userRepository.findByEmailIgnoreCase("blocked@example.com")).thenReturn(java.util.Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
        org.mockito.Mockito.verify(passwordEncoder, org.mockito.Mockito.never()).matches(any(), any());
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ana@example.com");
        request.setPassword("wrong");
        User user = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
        user.setActive(true);
        user.setPasswordHash("hash");
        when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldRejectLoginWhenPasswordHashIsNull() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ana@example.com");
        request.setPassword("Password123!");
        User user = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
        user.setActive(true);
        user.setPasswordHash(null);
        when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(java.util.Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
        org.mockito.Mockito.verify(passwordEncoder, org.mockito.Mockito.never()).matches(any(), any());
    }

    @Test
    void shouldFindAndSaveUsersThroughService() {
        User user = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
        when(userRepository.findById("client-1")).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertEquals(user, userService.findById("client-1"));
        assertEquals(user, userService.save(user));
    }

    @Test
    void shouldReturnAllUsers() {
        User first = new User("a@example.com", "A", "One", UserRole.CLIENT);
        User second = new User("b@example.com", "B", "Two", UserRole.SELLER);
        when(userRepository.findAll()).thenReturn(java.util.List.of(first, second));

        assertEquals(java.util.List.of(first, second), userService.findAllUsers());
    }

    @Test
    void shouldRejectSellerDeletionWhenHasPendingOrPreparingOrders() {
        User seller = new User(
                "seller@example.com",
                "Carlos",
                "López",
                UserRole.SELLER);

        ReflectionTestUtils.setField(seller, "id", "seller-1");

        Product product = org.mockito.Mockito.mock(Product.class);
        when(product.getId()).thenReturn("product-1");

        Order pendingOrder = org.mockito.Mockito.mock(Order.class);
        Order preparingOrder = org.mockito.Mockito.mock(Order.class);

        when(userRepository.findById("seller-1"))
                .thenReturn(Optional.of(seller));

        when(productRepository.findAllBySellerId("seller-1"))
                .thenReturn(List.of(product));

        when(orderRepository.findAllByItemsProductIdInAndStatusIn(
                List.of("product-1"),
                List.of(OrderStatus.PENDIENTE, OrderStatus.PREPARANDO)))
                .thenReturn(List.of(pendingOrder, preparingOrder));

        assertThrows(
                UserDeletionConflictException.class,
                () -> userService.deleteOwnAccount("seller-1"));

        // No se eliminan productos ni la cuenta.
        org.mockito.Mockito.verify(productRepository,
                org.mockito.Mockito.never()).deleteAll(any());

        org.mockito.Mockito.verify(userRepository,
                org.mockito.Mockito.never()).delete(any(User.class));

        // Tampoco se limpian los datos asociados a la cuenta.
        org.mockito.Mockito.verify(cartRepository,
                org.mockito.Mockito.never()).deleteByCustomerId(any());

        org.mockito.Mockito.verify(wishlistRepository,
                org.mockito.Mockito.never()).deleteAllByUserId(any());

        org.mockito.Mockito.verify(emailOtpChallengeRepository,
                org.mockito.Mockito.never()).deleteAllByUserId(any());

        org.mockito.Mockito.verify(passwordResetTokenRepository,
                org.mockito.Mockito.never()).deleteAllByUserId(any());

        org.mockito.Mockito.verify(loginAttemptRepository,
                org.mockito.Mockito.never()).deleteAllByEmail(any());
    }

    @Test
    void shouldDeleteSellerAndPreserveHistoricalOrders() {
        User seller = new User(
                "seller@example.com",
                "Carlos",
                "López",
                UserRole.SELLER);

        ReflectionTestUtils.setField(seller, "id", "seller-1");

        Product product = org.mockito.Mockito.mock(Product.class);
        when(product.getId()).thenReturn("product-1");

        Order historicalOrder = org.mockito.Mockito.mock(Order.class);

        when(userRepository.findById("seller-1"))
                .thenReturn(Optional.of(seller));

        when(productRepository.findAllBySellerId("seller-1"))
                .thenReturn(List.of(product));

        when(orderRepository.findAllByItemsProductIdInAndStatusIn(
                List.of("product-1"),
                List.of(OrderStatus.PENDIENTE, OrderStatus.PREPARANDO)))
                .thenReturn(List.of());

        when(orderRepository.findAllByItemsProductIdInAndStatusIn(
                List.of("product-1"),
                List.of(
                        OrderStatus.PAGADO,
                        OrderStatus.ENVIADO,
                        OrderStatus.ENTREGADO,
                        OrderStatus.CANCELADO)))
                .thenReturn(List.of(historicalOrder));

        when(cartRepository.findAll()).thenReturn(List.of());

        userService.deleteOwnAccount("seller-1");

        // Se conserva la asociación histórica con el vendedor.
        verify(historicalOrder).assignSellerToItems(
                "seller-1",
                List.of("product-1"));

        verify(orderRepository).save(historicalOrder);

        // Se eliminan los productos y sus referencias.
        verify(productRepository).deleteAll(List.of(product));
        verify(wishlistRepository).deleteAllByProductIdIn(List.of("product-1"));

        // Se limpian los datos propios del vendedor.
        verify(cartRepository).deleteByCustomerId("seller-1");
        verify(wishlistRepository).deleteAllByUserId("seller-1");

        // Se limpia la información de seguridad.
        verify(emailOtpChallengeRepository).deleteAllByUserId("seller-1");
        verify(passwordResetTokenRepository).deleteAllByUserId("seller-1");
        verify(loginAttemptRepository).deleteAllByEmail("seller@example.com");

        // Finalmente se elimina la cuenta.
        verify(userRepository).delete(seller);
    }

    @Test
    void shouldDeleteClientAndReassignHistoricalOrdersToTechnicalCustomer() {
        User client = new User(
                "ana@example.com",
                "Ana",
                "García",
                UserRole.CLIENT);

        ReflectionTestUtils.setField(client, "id", "client-1");

        Order historicalOrder = org.mockito.Mockito.mock(Order.class);

        when(userRepository.findById("client-1"))
                .thenReturn(Optional.of(client));

        when(userRepository.findByEmailIgnoreCase("cliente.eliminado@esibuy.local"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedUser, "id", "deleted-client");
            return savedUser;
        });

        when(orderRepository.findAllByCustomerIdOrderByCreatedAtDesc("client-1"))
                .thenReturn(List.of(historicalOrder));

        userService.deleteOwnAccount("client-1");

        // Se crea el usuario técnico.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User deletedCustomer = userCaptor.getValue();

        assertEquals("cliente.eliminado@esibuy.local", deletedCustomer.getEmail());
        assertEquals("Cliente", deletedCustomer.getFirstName());
        assertEquals("eliminado", deletedCustomer.getLastName());
        assertEquals(UserRole.CLIENT, deletedCustomer.getRole());
        assertFalse(deletedCustomer.isActive());
        assertTrue(deletedCustomer.isBlocked());
        assertNull(deletedCustomer.getPasswordHash());

        // Se conservan los pedidos, pero se anonimiza al cliente.
        verify(historicalOrder).setCustomerId("deleted-client");
        verify(historicalOrder).anonymizeCustomerData();
        verify(orderRepository).save(historicalOrder);

        // Se eliminan los datos propios del cliente.
        verify(cartRepository).deleteByCustomerId("client-1");
        verify(wishlistRepository).deleteAllByUserId("client-1");

        // Se limpian los datos de seguridad.
        verify(emailOtpChallengeRepository).deleteAllByUserId("client-1");
        verify(passwordResetTokenRepository).deleteAllByUserId("client-1");
        verify(loginAttemptRepository).deleteAllByEmail("ana@example.com");

        verify(userRepository).delete(client);
    }

    @Test
    void shouldRejectAdminAccountDeletion() {
        User admin = new User(
                "admin@example.com",
                "Admin",
                "Root",
                UserRole.ADMIN);

        ReflectionTestUtils.setField(admin, "id", "admin-1");

        when(userRepository.findById("admin-1"))
                .thenReturn(Optional.of(admin));

        assertThrows(
                UserDeletionConflictException.class,
                () -> userService.deleteOwnAccount("admin-1"));

        verify(userRepository,
                org.mockito.Mockito.never()).delete(any(User.class));

        verify(emailOtpChallengeRepository,
                org.mockito.Mockito.never()).deleteAllByUserId(any());

        verify(passwordResetTokenRepository,
                org.mockito.Mockito.never()).deleteAllByUserId(any());

        verify(loginAttemptRepository,
                org.mockito.Mockito.never()).deleteAllByEmail(any());
    }
}
