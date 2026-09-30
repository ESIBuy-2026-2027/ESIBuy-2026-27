package es.uclm.esibuy.users.service;

import java.time.LocalDate;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    // ============================================================
    // CLIENTES
    // ============================================================

    @Test
    void shouldRegisterClientSuccessfullyWithDefaultValues() {
        ClientRegistrationRequest request = new ClientRegistrationRequest();

        request.setFirstName(" Ana ");
        request.setLastName(" García ");
        request.setEmail("ANA@EXAMPLE.COM");
        request.setNationalId("12345678Z");
        request.setPhone("   ");
        request.setBirthDate(LocalDate.of(2000, 1, 1));
        request.setPassword("Password123!");
        request.setRepeatedPassword("Password123!");
        request.setCustomerType(CustomerType.STANDARD);
        request.setProfileImage("  ");

        when(userRepository.existsByEmailIgnoreCase("ana@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password123!"))
                .thenReturn("hashed-pwd");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerClient(request);

        assertNotNull(result);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        // Datos generales
        assertEquals("ana@example.com", savedUser.getEmail());
        assertEquals("Ana", savedUser.getFirstName());
        assertEquals("García", savedUser.getLastName());
        assertEquals("12345678Z", savedUser.getNationalId());

        // Datos opcionales / valores por defecto
        assertNull(savedUser.getPhone());
        assertEquals("avatar-client-default", savedUser.getProfileImage());

        // Seguridad
        assertEquals("hashed-pwd", savedUser.getPasswordHash());
        verify(passwordEncoder).encode("Password123!");

        // Rol y estado
        assertEquals(UserRole.CLIENT, savedUser.getRole());
        assertFalse(savedUser.isActive());

        // Perfil de cliente
        assertNotNull(savedUser.getClientProfile());
        assertEquals(
                CustomerType.STANDARD,
                savedUser.getClientProfile().getCustomerType());
        assertEquals(
                LocalDate.of(2000, 1, 1),
                savedUser.getClientProfile().getBirthDate());
    }

    @Test
    void shouldRegisterClientSuccessfullyWithCustomImageAndPhone() {
        ClientRegistrationRequest request = new ClientRegistrationRequest();

        request.setFirstName("Luis");
        request.setLastName("Pérez");
        request.setEmail("luis@example.com");
        request.setNationalId("87654321X");
        request.setPhone(" 600123456 ");
        request.setPassword("Pwd123!");
        request.setRepeatedPassword("Pwd123!");
        request.setCustomerType(CustomerType.PREMIUM);
        request.setProfileImage(" custom-avatar.png ");

        when(userRepository.existsByEmailIgnoreCase("luis@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Pwd123!"))
                .thenReturn("hash");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerClient(request);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("600123456", savedUser.getPhone());
        assertEquals("custom-avatar.png", savedUser.getProfileImage());
        assertEquals(UserRole.CLIENT, savedUser.getRole());
        assertFalse(savedUser.isActive());
        assertEquals("hash", savedUser.getPasswordHash());
    }

    @Test
    void shouldThrowDuplicateUserWhenEmailExistsOnClientRegister() {
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setEmail("ana@example.com");

        when(userRepository.existsByEmailIgnoreCase("ana@example.com"))
                .thenReturn(true);

        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> userService.registerClient(request));

        assertEquals(
                "Ya existe un usuario con ese email",
                exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    // ============================================================
    // VENDEDORES
    // ============================================================

    @Test
    void shouldRegisterSellerSuccessfullyWithDefaultImage() {
        SellerRegistrationRequest request = new SellerRegistrationRequest();

        request.setFirstName(" Carlos ");
        request.setLastName(" Lopez ");
        request.setEmail("CARLOS@TECH.COM");
        request.setNationalId("11111111H");
        request.setBusinessName(" Carlos Tech SL ");
        request.setDefaultCategory(" Informática ");
        request.setPassword("Pwd123!");
        request.setRepeatedPassword("Pwd123!");

        when(userRepository.existsByEmailIgnoreCase("carlos@tech.com"))
                .thenReturn(false);

        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase(
                "Carlos Tech SL"))
                .thenReturn(false);

        when(passwordEncoder.encode("Pwd123!"))
                .thenReturn("hash");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerSeller(request);

        assertNotNull(result);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        // Datos generales
        assertEquals("carlos@tech.com", savedUser.getEmail());
        assertEquals("Carlos", savedUser.getFirstName());
        assertEquals("Lopez", savedUser.getLastName());
        assertEquals("11111111H", savedUser.getNationalId());

        // Valores por defecto
        assertEquals(
                "avatar-seller-default",
                savedUser.getProfileImage());

        // Seguridad
        assertEquals("hash", savedUser.getPasswordHash());
        verify(passwordEncoder).encode("Pwd123!");

        // Rol y estado
        assertEquals(UserRole.SELLER, savedUser.getRole());
        assertFalse(savedUser.isActive());

        // Perfil de vendedor
        assertNotNull(savedUser.getSellerProfile());
        assertEquals(
                "Carlos Tech SL",
                savedUser.getSellerProfile().getBusinessName());
        assertEquals(
                "Informática",
                savedUser.getSellerProfile().getDefaultCategory());
    }

    @Test
    void shouldThrowDuplicateUserWhenEmailExistsOnSellerRegister() {
        SellerRegistrationRequest request = new SellerRegistrationRequest();
        request.setEmail("carlos@tech.com");

        when(userRepository.existsByEmailIgnoreCase("carlos@tech.com"))
                .thenReturn(true);

        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> userService.registerSeller(request));

        assertEquals(
                "Ya existe un usuario con ese email",
                exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldThrowDuplicateUserWhenBusinessNameExistsOnSellerRegister() {
        SellerRegistrationRequest request = new SellerRegistrationRequest();

        request.setEmail("nuevo@tech.com");
        request.setBusinessName("Carlos Tech SL");

        when(userRepository.existsByEmailIgnoreCase("nuevo@tech.com"))
                .thenReturn(false);

        when(userRepository.existsBySellerProfileBusinessNameIgnoreCase(
                "Carlos Tech SL"))
                .thenReturn(true);

        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> userService.registerSeller(request));

        assertEquals(
                "Ya existe un vendedor con ese nombre comercial",
                exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    // ============================================================
    // ADMINISTRADORES
    // ============================================================

    @Test
    void shouldCreateAdminSuccessfullyAndActive() {
        AdminCreateRequest request = new AdminCreateRequest();

        request.setFirstName("Admin");
        request.setLastName("Root");
        request.setEmail("ADMIN@ESIBUY.COM");
        request.setPassword("Admin123!Safe");
        request.setHeadquarters(" Ciudad Real ");

        when(userRepository.existsByEmailIgnoreCase("admin@esibuy.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Admin123!Safe"))
                .thenReturn("hashed-admin-pwd");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createAdmin(request);

        assertNotNull(result);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        // Datos generales
        assertEquals("admin@esibuy.com", savedUser.getEmail());
        assertEquals("Admin", savedUser.getFirstName());
        assertEquals("Root", savedUser.getLastName());

        // Valores por defecto
        assertEquals(
                "avatar-admin-default",
                savedUser.getProfileImage());

        // Seguridad
        assertEquals(
                "hashed-admin-pwd",
                savedUser.getPasswordHash());
        verify(passwordEncoder).encode("Admin123!Safe");

        // Rol y estado
        assertEquals(UserRole.ADMIN, savedUser.getRole());
        assertTrue(savedUser.isActive());

        // Perfil de administrador
        assertNotNull(savedUser.getAdminProfile());
        assertEquals(
                "Ciudad Real",
                savedUser.getAdminProfile().getHeadquarters());
        assertNotNull(savedUser.getAdminProfile().getHireDate());
    }

    @Test
    void shouldThrowDuplicateUserWhenEmailExistsOnAdminCreate() {
        AdminCreateRequest request = new AdminCreateRequest();
        request.setEmail("admin@esibuy.com");

        when(userRepository.existsByEmailIgnoreCase("admin@esibuy.com"))
                .thenReturn(true);

        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> userService.createAdmin(request));

        assertEquals(
                "Ya existe un usuario con ese email",
                exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRegisterClientSuccessfullyWithStrictNullValues() {
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setFirstName("Ana");
        request.setLastName("García");
        request.setEmail("ana2@example.com");
        request.setNationalId("12345678Z");
        request.setPassword("Password123!");
        request.setRepeatedPassword("Password123!");
        request.setCustomerType(CustomerType.STANDARD);

        // Forzando el valor nulo estricto para cubrir las ramas izquierdas de los
        // condicionales
        request.setPhone(null);
        request.setProfileImage(null);

        when(userRepository.existsByEmailIgnoreCase("ana2@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.registerClient(request);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNull(savedUser.getPhone());
        assertEquals("avatar-client-default", savedUser.getProfileImage());
    }
}