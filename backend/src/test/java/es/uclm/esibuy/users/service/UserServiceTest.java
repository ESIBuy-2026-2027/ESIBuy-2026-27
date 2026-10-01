package es.uclm.esibuy.users.service;

import java.time.LocalDate;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
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
    void shouldAuthenticateUserWithEmailAndPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" SELLER@example.com ");
        request.setPassword("Password123!");
        User seller = new User("seller@example.com", "Carlos", "Lopez", UserRole.SELLER);
        seller.setPasswordHash("hashed-pwd");

        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(java.util.Optional.of(seller));
        when(passwordEncoder.matches("Password123!", "hashed-pwd")).thenReturn(true);

        assertEquals(seller, userService.login(request));
    }

    @Test
    void shouldRejectInvalidLoginCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("seller@example.com");
        request.setPassword("wrong-password");
        User seller = new User("seller@example.com", "Carlos", "Lopez", UserRole.SELLER);
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
}
