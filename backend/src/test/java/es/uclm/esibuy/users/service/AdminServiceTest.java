package es.uclm.esibuy.users.service;

import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.CandidatoDTO;
import es.uclm.esibuy.users.dto.PromoverAdminDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository);
    }

    @Test
    void shouldObtenerCandidatosSuccessfully() {
        User cliente = new User("cliente@esibuy.com", "Laura", "Gómez", UserRole.CLIENT);
        User vendedor = new User("vendedor@esibuy.com", "Mario", "Santos", UserRole.SELLER);

        when(userRepository.findByRoleIn(List.of(UserRole.CLIENT, UserRole.SELLER)))
                .thenReturn(List.of(cliente, vendedor));

        List<CandidatoDTO> candidatos = adminService.obtenerCandidatos();

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

        User result = adminService.promoverAAdmin("usr-123", request);

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
        // Regla: Sede y fecha de incorporacion tomada del registro en BD (createdAt)
        assertNotNull(result.getAdminProfile());
        assertEquals("Ciudad Real", result.getAdminProfile().getHeadquarters());
        assertEquals(java.time.LocalDateTime.ofInstant(existingUser.getCreatedAt(), java.time.ZoneOffset.UTC), result.getAdminProfile().getHireDate());

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

        User result = adminService.promoverAAdmin("usr-456", request);

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

        assertThrows(UserNotFoundException.class, () -> adminService.promoverAAdmin("non-existent-id", request));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenPromotingAlreadyAdminUser() {
        User adminUser = new User("admin@esibuy.com", "Super", "Admin", UserRole.ADMIN);

        PromoverAdminDTO request = new PromoverAdminDTO();
        request.setNombre("Super");
        request.setApellidos("Admin");

        when(userRepository.findById("admin-id")).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class, () -> adminService.promoverAAdmin("admin-id", request));
    }
}
