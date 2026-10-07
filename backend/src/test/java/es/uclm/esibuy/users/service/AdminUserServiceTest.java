package es.uclm.esibuy.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, productRepository);
    }

    @Test
    void updatesOnlyWhitelistedProperties() {
        User existing = new User("cliente@example.com", "Old", "Name", UserRole.CLIENT);
        existing.setPhone("600111222");
        existing.setPasswordHash("encoded-secret");
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setFirstName(" New ");
        request.setLastName(" Name ");
        request.setPhone("600111222");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.actualizar("user-1", request);

        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals("600111222", result.getPhone());
        assertEquals(UserRole.CLIENT, result.getRole());
        assertEquals("encoded-secret", result.getPasswordHash());
        assertEquals("cliente@example.com", result.getEmail());
        verify(userRepository).save(existing);
    }

    @Test
    void failsWhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.obtenerPorId("missing"));
    }

    @Test
    void deletesExistingUserWhenIdIsValidAndItIsNotTheAuthenticatedAdmin() {
        String userId = "507f1f77bcf86cd799439011";
        User user = new User("cliente@example.com", "Ana", "López", UserRole.CLIENT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.eliminar(userId, "507f1f77bcf86cd799439012");

        verify(userRepository).delete(user);
    }

    @Test
    void rejectsMalformedIdWithoutAccessingRepository() {
        assertThrows(IllegalStateException.class, () -> service.eliminar("not-an-object-id", "admin-id"));

        verify(userRepository, never()).findById("not-an-object-id");
    }

    @Test
    void preventsAdministratorFromDeletingTheirOwnAccount() {
        String adminId = "507f1f77bcf86cd799439011";
        String alternateCaseAdminId = adminId.toUpperCase(Locale.ROOT);

        assertThrows(IllegalStateException.class,
                () -> service.eliminar(alternateCaseAdminId, adminId));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void doesNotDeleteWhenUserDoesNotExist() {
        String userId = "507f1f77bcf86cd799439011";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.eliminar(userId, "507f1f77bcf86cd799439012"));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void refusesToDeleteSellerWithProducts() {
        String sellerId = "507f1f77bcf86cd799439011";
        User seller = new User("seller@example.com", "Vera", "Seller", UserRole.SELLER);
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(productRepository.countBySellerId(sellerId)).thenReturn(1L);

        assertThrows(UserDeletionConflictException.class,
                () -> service.eliminar(sellerId, "507f1f77bcf86cd799439012"));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deletesSellerWithoutProducts() {
        String sellerId = "507f1f77bcf86cd799439011";
        User seller = new User("seller@example.com", "Vera", "Seller", UserRole.SELLER);
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(productRepository.countBySellerId(sellerId)).thenReturn(0L);

        service.eliminar(sellerId, "507f1f77bcf86cd799439012");

        verify(userRepository).delete(seller);
    }
}