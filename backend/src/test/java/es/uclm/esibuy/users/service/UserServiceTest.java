package es.uclm.esibuy.users.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.AdminCreateRequest;
import es.uclm.esibuy.users.dto.CandidatoDTO;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.PromoverAdminDTO;
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
    void shouldObtenerCandidatosSuccessfully() {
        User cliente = new User("cliente@esibuy.com", "Laura", "Gómez", UserRole.CLIENT);
        User vendedor = new User("vendedor@esibuy.com", "Mario", "Santos", UserRole.SELLER);

        when(userRepository.findByRoleIn(List.of(UserRole.CLIENT, UserRole.SELLER)))
                .thenReturn(List.of(cliente, vendedor));

        List<CandidatoDTO> candidatos = userService.obtenerCandidatos();

        assertNotNull(candidatos);
        assertEquals(2, candidatos.size());
        assertEquals("Laura", candidatos.get(0).getNombre());
        assertEquals("CLIENT", candidatos.get(0).getRol());
        assertEquals("Mario", candidatos.get(1).getNombre());
        assertEquals("SELLER", candidatos.get(1).getRol());
    }

    @Test
    void shouldPromoverAAdminSuccessfullyWithProtectedEmailAndDefaultAvatar() {
        User existingUser = new User("usuario.real@esibuy.com", "Pedro", "Navarro", UserRole.CLIENT);
        existingUser.setActive(false);

        PromoverAdminDTO request = new PromoverAdminDTO();
        request.setNombre("Pedro Actualizado");
        request.setApellidos("Navarro Actualizado");
        request.setEmail("intento.modificar.email@esibuy.com"); // Debe ser ignorado
        request.setSede("Ciudad Real");
        request.setImagenPerfil(null); // Debe asignar avatar por defecto

        when(userRepository.findById("usr-123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.promoverAAdmin("usr-123", request);

        assertNotNull(result);
        // Regla: Proteger email de sobrescritura
        assertEquals("usuario.real@esibuy.com", result.getEmail());
        // Regla: Actualizar nombre y apellidos
        assertEquals("Pedro Actualizado", result.getFirstName());
        assertEquals("Navarro Actualizado", result.getLastName());
        // Regla: Rol ADMIN y activo true
        assertEquals(UserRole.ADMIN, result.getRole());
        assertTrue(result.isActive());
        // Regla: Avatar por defecto si nulo
        assertEquals("avatar-admin-default", result.getProfileImage());
        // Regla: Sede y fecha de incorporacion autogenerada
        assertNotNull(result.getAdminProfile());
        assertEquals("Ciudad Real", result.getAdminProfile().getHeadquarters());
        assertNotNull(result.getAdminProfile().getHireDate());

        verify(userRepository).save(existingUser);
    }

    @Test
    void shouldPromoverAAdminWithCustomProfileImage() {
        User existingUser = new User("vendedor@esibuy.com", "Marta", "Pérez", UserRole.SELLER);
        existingUser.setActive(false);

        PromoverAdminDTO request = new PromoverAdminDTO();
        request.setNombre("Marta");
        request.setApellidos("Pérez");
        request.setSede("Toledo");
        request.setImagenPerfil("https://avatar.ejemplo.com/marta.png");

        when(userRepository.findById("usr-456")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.promoverAAdmin("usr-456", request);

        assertNotNull(result);
        assertEquals("https://avatar.ejemplo.com/marta.png", result.getProfileImage());
        assertEquals(UserRole.ADMIN, result.getRole());
        assertTrue(result.isActive());
        assertEquals("Toledo", result.getAdminProfile().getHeadquarters());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenPromotingNonExistentUser() {
        PromoverAdminDTO request = new PromoverAdminDTO();
        request.setNombre("Inexistente");
        request.setApellidos("Usuario");

        when(userRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.promoverAAdmin("non-existent-id", request));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenPromotingAlreadyAdminUser() {
        User adminUser = new User("admin@esibuy.com", "Super", "Admin", UserRole.ADMIN);

        PromoverAdminDTO request = new PromoverAdminDTO();
        request.setNombre("Super");
        request.setApellidos("Admin");

        when(userRepository.findById("admin-id")).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class, () -> userService.promoverAAdmin("admin-id", request));
    }
}
