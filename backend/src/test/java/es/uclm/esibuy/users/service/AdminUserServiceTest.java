package es.uclm.esibuy.users.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository);
    }

    @Test
    void updatesOnlyWhitelistedProperties() {
        User existing = new User("cliente@example.com", "Old", "Name", UserRole.CLIENT);
        existing.setPhone("600111222");
        existing.setPasswordHash("encoded-secret");
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setFirstName(" New ");
        request.setLastName(" Name ");
        request.setNationalId("12345678A");
        request.setPhone("600111222");
        request.setProfileImage("assets/images/avatars/av_client_2.jpeg");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.actualizar("user-1", request);

        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals("600111222", result.getPhone());
        assertEquals("assets/images/avatars/av_client_2.jpeg", result.getProfileImage());
        assertEquals(UserRole.CLIENT, result.getRole());
        assertEquals("encoded-secret", result.getPasswordHash());
        assertEquals("cliente@example.com", result.getEmail());
        verify(userRepository).save(existing);
    }

    @Test
    void updatesAdminHeadquarters() {
        User admin = new User("admin@example.com", "Admin", "User", UserRole.ADMIN);
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setNationalId("12345678Z");
        request.setHeadquarters("Ciudad Real");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.actualizar("admin-1", request);

        assertEquals("12345678Z", result.getNationalId());
        assertEquals("Ciudad Real", result.getAdminProfile().getHeadquarters());
    }

    @Test
    void updatesSellerBusinessNameAndDefaultCategory() {
        User seller = new User("seller@example.com", "Seller", "User", UserRole.SELLER);
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setFirstName("Seller");
        request.setLastName("User");
        request.setNationalId("87654321X");
        request.setBusinessName("Mi Tienda Nueva");
        request.setDefaultCategory("Electrónica");
        when(userRepository.findById("seller-1")).thenReturn(Optional.of(seller));
        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase("Mi Tienda Nueva")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.actualizar("seller-1", request);

        assertEquals("87654321X", result.getNationalId());
        assertEquals("Mi Tienda Nueva", result.getSellerProfile().getBusinessName());
        assertEquals("Electrónica", result.getSellerProfile().getDefaultCategory());
    }

    @Test
    void updatesClientBirthDate() {
        User client = new User("client@example.com", "Client", "User", UserRole.CLIENT);
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setFirstName("Client");
        request.setLastName("User");
        request.setBirthDate(LocalDate.of(1995, 5, 20));
        when(userRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.actualizar("client-1", request);

        assertEquals(LocalDate.of(1995, 5, 20), result.getClientProfile().getBirthDate());
    }

    @Test
    void doesNotUpdateAdminPhone() {
        User admin = new User("admin@example.com", "Admin", "User", UserRole.ADMIN);
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setPhone("699888777");
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.actualizar("admin-1", request);

        assertNull(result.getPhone());
    }

    @Test
    void failsWhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.obtenerPorId("missing"));
    }

    @Test
    void deletesAnotherAdministratorWhenIdIsValid() {
        String userId = "507f1f77bcf86cd799439011";
        User user = new User("admin@example.com", "Ana", "López", UserRole.ADMIN);
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
    void refusesToDeleteClientAccount() {
        String clientId = "507f1f77bcf86cd799439011";
        User client = new User("client@example.com", "Ana", "Client", UserRole.CLIENT);
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));

        assertThrows(UserDeletionConflictException.class,
                () -> service.eliminar(clientId, "507f1f77bcf86cd799439012"));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void refusesToDeleteSellerAccountEvenWithoutProducts() {
        String sellerId = "507f1f77bcf86cd799439011";
        User seller = new User("seller@example.com", "Vera", "Seller", UserRole.SELLER);
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        assertThrows(UserDeletionConflictException.class,
                () -> service.eliminar(sellerId, "507f1f77bcf86cd799439012"));

        verify(userRepository, never()).delete(any(User.class));
    }
}
