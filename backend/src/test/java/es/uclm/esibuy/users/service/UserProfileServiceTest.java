package es.uclm.esibuy.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.UserProfileResponse;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.users.dto.UpdateOwnProfileRequestDTO;
import es.uclm.esibuy.users.model.SellerProfile;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, productRepository);
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
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateOwnProfile("missing", request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldUpdateSellerBusinessNameWhenItIsNotDuplicated() {
        User seller = new User(
                "luis@example.com", "Luis", "Martín", UserRole.SELLER);
        seller.setSellerProfile(new SellerProfile("Tienda Luis", "Electrónica"));

        when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Tienda Luis 2"))
                .thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        User seller = new User(
                "luis@example.com", "Luis", "Martín", UserRole.SELLER);
        seller.setSellerProfile(new SellerProfile("Tienda Luis", "Electrónica"));

        when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Otra Tienda"))
                .thenReturn(true);

        UpdateOwnProfileRequestDTO request = new UpdateOwnProfileRequestDTO();
        request.setFirstName("Luis");
        request.setLastName("Martín");
        request.setBusinessName("Otra Tienda");

        assertThrows(
                DuplicateUserException.class,
                () -> userService.updateOwnProfile("seller-1", request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDeleteClientAccountSuccessfully() {
        User client = new User(
                "ana@example.com", "Ana", "García", UserRole.CLIENT);
        when(userRepository.findById("client-1")).thenReturn(Optional.of(client));

        userService.deleteOwnAccount("client-1");

        verify(userRepository).delete(client);
        verify(productRepository, never()).deleteAll(any());
    }

    @Test
    void shouldDeleteSellerProductsBeforeDeletingAccount() {
        User seller = org.mockito.Mockito.mock(User.class);
        when(seller.getId()).thenReturn("seller-1");
        when(seller.getRole()).thenReturn(UserRole.SELLER);
        when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));

        when(productRepository.findAllBySellerId("seller-1"))
                .thenReturn(java.util.Collections.emptyList());

        userService.deleteOwnAccount("seller-1");

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                productRepository, userRepository);

        inOrder.verify(productRepository).findAllBySellerId("seller-1");
        inOrder.verify(productRepository).deleteAll(any());
        inOrder.verify(userRepository).delete(seller);
    }

    @Test
    void shouldRejectAdminSelfDeletion() {
        User admin = new User(
                "admin@example.com", "Admin", "Root", UserRole.ADMIN);
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userService.deleteOwnAccount("admin-1"));

        assertEquals(
                true,
                exception.getMessage().contains(
                        "No puedes eliminar tu propia cuenta de administrador"));

        verify(userRepository, never()).delete(any(User.class));
        verify(productRepository, never()).deleteAll(any());
    }

    @Test
    void shouldThrowWhenDeletingAccountOfMissingUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteOwnAccount("missing"));

        verify(userRepository, never()).delete(any(User.class));
    }
}