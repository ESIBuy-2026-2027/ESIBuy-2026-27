package es.uclm.esibuy.users.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.auth.passwordreset.repository.PasswordResetTokenRepository;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.security.repository.EmailOtpChallengeRepository;
import es.uclm.esibuy.security.repository.LoginAttemptRepository;
import es.uclm.esibuy.users.dto.UpdateOwnProfileRequestDTO;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
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
                userService = new UserService(userRepository, passwordEncoder, productRepository,
                                orderRepository, cartRepository, wishlistRepository,
                                emailOtpChallengeRepository, passwordResetTokenRepository, loginAttemptRepository);
        }

        @Test
        void shouldReturnOwnProfileWithMaskedNationalId() {
                User user = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
                user.setNationalId("12345678Z");
                when(userRepository.findById("client-1")).thenReturn(Optional.of(user));
                UserProfileResponse profile = userService.getOwnProfile("client-1");
                assertEquals("ana@example.com", profile.email());
                assertEquals("*****678Z", profile.maskedNationalId());
        }

        @Test
        void shouldThrowWhenUserDoesNotExist() {
                when(userRepository.findById("missing")).thenReturn(Optional.empty());
                assertThrows(UserNotFoundException.class, () -> userService.getOwnProfile("missing"));
        }

        @Test
        void shouldUpdateOwnProfileSuccessfully() {
                User user = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
                user.setNationalId("12345678Z");
                when(userRepository.findById("client-1")).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
                UpdateOwnProfileRequestDTO request = new UpdateOwnProfileRequestDTO();
                request.setFirstName("Ana María");
                request.setLastName("García López");
                request.setPhone("611222333");
                request.setProfileImage("avatar-client-default");
                UserProfileResponse result = userService.updateOwnProfile("client-1", request);
                assertEquals("Ana María", result.firstName());
                assertEquals("García López", result.lastName());
                assertEquals("611222333", result.phone());
                assertEquals("*****678Z", result.maskedNationalId());
                verify(userRepository).save(user);
        }

        @Test
        void shouldThrowWhenUpdatingProfileOfMissingUser() {
                when(userRepository.findById("missing")).thenReturn(Optional.empty());
                UpdateOwnProfileRequestDTO request = new UpdateOwnProfileRequestDTO();
                request.setFirstName("Ana");
                request.setLastName("García");
                assertThrows(UserNotFoundException.class, () -> userService.updateOwnProfile("missing", request));
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldUpdateSellerBusinessNameWhenItIsNotDuplicated() {
                User seller = new User("luis@example.com", "Luis", "Martín", UserRole.SELLER);
                seller.setSellerProfile(new SellerProfile("Tienda Luis", "Electrónica"));
                when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
                when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Tienda Luis 2")).thenReturn(false);
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
                UpdateOwnProfileRequestDTO request = new UpdateOwnProfileRequestDTO();
                request.setFirstName("Luis");
                request.setLastName("Martín");
                request.setBusinessName("Tienda Luis 2");
                request.setDefaultCategory("Informática");
                UserProfileResponse result = userService.updateOwnProfile("seller-1", request);
                assertEquals("Tienda Luis 2", result.businessName());
                assertEquals("Informática", result.defaultCategory());
                verify(userRepository).save(seller);
        }

        @Test
        void shouldRejectDuplicatedSellerBusinessName() {
                User seller = new User("luis@example.com", "Luis", "Martín", UserRole.SELLER);
                seller.setSellerProfile(new SellerProfile("Tienda Luis", "Electrónica"));
                when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
                when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Otra Tienda")).thenReturn(true);
                UpdateOwnProfileRequestDTO request = new UpdateOwnProfileRequestDTO();
                request.setFirstName("Luis");
                request.setLastName("Martín");
                request.setBusinessName("Otra Tienda");
                assertThrows(DuplicateUserException.class, () -> userService.updateOwnProfile("seller-1", request));
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void shouldDeleteClientAccountAndAnonymizeHistoricalOrders() {
                User client = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
                ReflectionTestUtils.setField(client, "id", "client-1");
                User deletedCustomer = mock(User.class);
                when(deletedCustomer.getId()).thenReturn("deleted-customer-id");
                Order order = mock(Order.class);
                when(userRepository.findById("client-1")).thenReturn(Optional.of(client));
                when(userRepository.findByEmailIgnoreCase("cliente.eliminado@esibuy.local"))
                                .thenReturn(Optional.of(deletedCustomer));
                when(orderRepository.findAllByCustomerIdOrderByCreatedAtDesc("client-1")).thenReturn(List.of(order));

                userService.deleteOwnAccount("client-1");

                verify(order).setCustomerId("deleted-customer-id");
                verify(order).anonymizeCustomerData();
                verify(orderRepository).save(order);
                verify(cartRepository).deleteByCustomerId("client-1");
                verify(wishlistRepository).deleteAllByUserId("client-1");
                verifySecurityDataCleanup(client);
                verify(userRepository).delete(client);
        }

        @Test
        void shouldCreateTechnicalCustomerWhenItDoesNotExist() {
                User client = new User("ana@example.com", "Ana", "García", UserRole.CLIENT);
                ReflectionTestUtils.setField(client, "id", "client-1");
                when(userRepository.findById("client-1")).thenReturn(Optional.of(client));
                when(userRepository.findByEmailIgnoreCase("cliente.eliminado@esibuy.local"))
                                .thenReturn(Optional.empty());
                when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                        return inv.getArgument(0);
                });
                when(orderRepository.findAllByCustomerIdOrderByCreatedAtDesc("client-1")).thenReturn(List.of());

                userService.deleteOwnAccount("client-1");

                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(captor.capture());
                User technical = captor.getValue();
                assertEquals("cliente.eliminado@esibuy.local", technical.getEmail());
                assertEquals("Cliente", technical.getFirstName());
                assertEquals("eliminado", technical.getLastName());
                assertEquals(UserRole.CLIENT, technical.getRole());
                assertFalse(technical.isActive());
                assertTrue(technical.isBlocked());
                assertNull(technical.getPasswordHash());
        }

        @Test
        void shouldDeleteSellerProductsAndKeepHistoricalSellerOrders() {
                User seller = new User("seller@example.com", "Carlos", "López", UserRole.SELLER);
                Product product = mock(Product.class);
                when(product.getId()).thenReturn("product-1");
                Order historicalOrder = mock(Order.class);
                when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
                // La entidad real obtiene el id desde Mongo; para esta prueba usamos el id del
                // vendedor mediante spy.
                seller = spy(seller);
                when(seller.getId()).thenReturn("seller-1");
                when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
                when(productRepository.findAllBySellerId("seller-1")).thenReturn(List.of(product));
                when(orderRepository.findAllByItemsProductIdInAndStatusIn(eq(List.of("product-1")), anyList()))
                                .thenReturn(List.of(), List.of(historicalOrder));
                when(cartRepository.findAll()).thenReturn(List.of());

                userService.deleteOwnAccount("seller-1");

                verify(historicalOrder).assignSellerToItems("seller-1", List.of("product-1"));
                verify(orderRepository).save(historicalOrder);
                verify(wishlistRepository).deleteAllByProductIdIn(List.of("product-1"));
                verify(productRepository).deleteAll(List.of(product));
                verify(cartRepository).deleteByCustomerId("seller-1");
                verify(wishlistRepository).deleteAllByUserId("seller-1");
                verify(userRepository).delete(seller);
                verifySecurityDataCleanup(seller);
        }

        @Test
        void shouldRejectSellerDeletionWhenThereArePendingOrPreparingOrders() {
                User seller = spy(new User("seller@example.com", "Carlos", "López", UserRole.SELLER));
                when(seller.getId()).thenReturn("seller-1");
                Product product = mock(Product.class);
                when(product.getId()).thenReturn("product-1");
                Order blockingOrder = mock(Order.class);
                when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
                when(productRepository.findAllBySellerId("seller-1")).thenReturn(List.of(product));
                when(orderRepository.findAllByItemsProductIdInAndStatusIn(
                                List.of("product-1"), List.of(OrderStatus.PENDIENTE, OrderStatus.PREPARANDO)))
                                .thenReturn(List.of(blockingOrder));

                assertThrows(UserDeletionConflictException.class, () -> userService.deleteOwnAccount("seller-1"));
                verify(productRepository, never()).deleteAll(anyList());
                verify(userRepository, never()).delete(any(User.class));
                verify(cartRepository, never()).deleteByCustomerId("seller-1");
                verify(loginAttemptRepository, never()).deleteAllByEmail("seller@example.com");
        }

        @Test
        void shouldDeleteSellerAccountWhenItHasNoProducts() {
                User seller = spy(new User("seller@example.com", "Carlos", "López", UserRole.SELLER));
                when(seller.getId()).thenReturn("seller-1");
                when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
                when(productRepository.findAllBySellerId("seller-1")).thenReturn(List.of());

                userService.deleteOwnAccount("seller-1");

                verify(productRepository, never()).deleteAll(anyList());
                verify(orderRepository, never()).findAllByItemsProductIdInAndStatusIn(anyList(), anyList());
                verify(cartRepository).deleteByCustomerId("seller-1");
                verify(wishlistRepository).deleteAllByUserId("seller-1");
                verify(userRepository).delete(seller);
        }

        @Test
        void shouldRejectAdminSelfDeletion() {
                User admin = new User("admin@example.com", "Admin", "Root", UserRole.ADMIN);
                when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
                UserDeletionConflictException exception = assertThrows(UserDeletionConflictException.class,
                                () -> userService.deleteOwnAccount("admin-1"));
                assertTrue(exception.getMessage().contains("No puedes eliminar tu propia cuenta de administrador"));
                verify(userRepository, never()).delete(any(User.class));
                verify(emailOtpChallengeRepository, never()).deleteAllByUserId(anyString());
        }

        @Test
        void shouldThrowWhenDeletingAccountOfMissingUser() {
                when(userRepository.findById("missing")).thenReturn(Optional.empty());
                assertThrows(UserNotFoundException.class, () -> userService.deleteOwnAccount("missing"));
                verify(userRepository, never()).delete(any(User.class));
        }

        private void verifySecurityDataCleanup(User user) {
                verify(emailOtpChallengeRepository).deleteAllByUserId(user.getId());
                verify(passwordResetTokenRepository).deleteAllByUserId(user.getId());
                verify(loginAttemptRepository).deleteAllByEmail(user.getEmail());
        }
}
