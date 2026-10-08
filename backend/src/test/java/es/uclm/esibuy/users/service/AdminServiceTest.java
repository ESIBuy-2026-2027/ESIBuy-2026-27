package es.uclm.esibuy.users.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.AltaAdminDTO;
import es.uclm.esibuy.users.dto.UserAdminDTO;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, passwordEncoder);
    }

    @Test
    void shouldObtenerTodosLosUsuariosSuccessfully() {
        User u1 = new User("admin@esibuy.com", "Admin", "Principal", UserRole.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(u1));

        List<UserAdminDTO> result = adminService.obtenerTodosLosUsuarios();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("admin@esibuy.com", result.get(0).getEmail());
    }

    @Test
    void shouldCreateAdminSuccessfully() {
        AltaAdminDTO request = new AltaAdminDTO(
                "12345678Z", "Carlos", "Gómez Pérez", "carlos.admin@esibuy.com",
                "Admin123456!#", "Admin123456!#", "Ciudad Real", "https://avatar.example/carlos.png");

        when(userRepository.existsByEmailIgnoreCase("carlos.admin@esibuy.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin123456!#")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = adminService.crearAdministrador(request);

        assertNotNull(created);
        assertEquals("carlos.admin@esibuy.com", created.getEmail());
        assertEquals(UserRole.ADMIN, created.getRole());
        assertTrue(created.isActive());
        assertEquals("hashed-pass", created.getPasswordHash());
        assertEquals("https://avatar.example/carlos.png", created.getProfileImage());
        assertNotNull(created.getAdminProfile());
        assertEquals("Ciudad Real", created.getAdminProfile().getHeadquarters());
    }

    @Test
    void shouldFilterUsersByRoleAndStatus() {
        User activeClient = new User("client@example.com", "Ana", "López", UserRole.CLIENT);
        activeClient.setActive(true);

        User inactiveSeller = new User("seller@example.com", "Luis", "Martín", UserRole.SELLER);
        inactiveSeller.setActive(false);

        User blockedAdmin = new User("admin@example.com", "Patricia", "García", UserRole.ADMIN);
        blockedAdmin.setBlocked(true);
        blockedAdmin.setActive(false);

        when(userRepository.findAll()).thenReturn(List.of(activeClient, inactiveSeller, blockedAdmin));

        List<UserAdminDTO> result = adminService.obtenerUsuariosFiltrados("CLIENTE", "ACTIVO");

        assertEquals(1, result.size());
        assertEquals("client@example.com", result.get(0).getEmail());
    }

    @Test
    void shouldActivateUserWhenNotBlocked() {
        User user = new User("client@example.com", "Ana", "López", UserRole.CLIENT);
        user.setActive(false);
        user.setBlocked(false);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminService.activarUsuario("user-1");

        assertTrue(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void shouldNotActivateBlockedUser() {
        User user = new User("client@example.com", "Ana", "López", UserRole.CLIENT);
        user.setActive(false);
        user.setBlocked(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        adminService.activarUsuario("user-1");

        assertFalse(user.isActive());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDeactivateUser() {
        User user = new User("client@example.com", "Ana", "López", UserRole.CLIENT);
        user.setActive(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminService.desactivarUsuario("user-1");

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void shouldBlockUser() {
        User user = new User("client@example.com", "Ana", "López", UserRole.CLIENT);
        user.setActive(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminService.bloquearUsuario("user-1");

        assertFalse(user.isActive());
        assertTrue(user.isBlocked());
        verify(userRepository).save(user);
    }

    @Test
    void shouldUnblockUser() {
        User user = new User("client@example.com", "Ana", "López", UserRole.CLIENT);
        user.setActive(false);
        user.setBlocked(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminService.desbloquearUsuario("user-1");

        assertTrue(user.isActive());
        assertFalse(user.isBlocked());
        verify(userRepository).save(user);
    }

    @Test
    void shouldFilterUsersByStatusAndHandleNullOrUnsupportedRole() {
        User activeClient = new User("client@example.com", "Ana", "López", UserRole.CLIENT);
        activeClient.setActive(true);

        User inactiveSeller = new User("seller@example.com", "Luis", "Martín", UserRole.SELLER);
        inactiveSeller.setActive(false);

        User blockedAdmin = new User("admin@example.com", "Patricia", "García", UserRole.ADMIN);
        blockedAdmin.setBlocked(true);
        blockedAdmin.setActive(false);

        User noRoleUser = new User("norole@example.com", "Sin", "Rol", UserRole.CLIENT);
        noRoleUser.setRole(null);

        when(userRepository.findAll()).thenReturn(List.of(activeClient, inactiveSeller, blockedAdmin, noRoleUser));

        assertEquals(1, adminService.obtenerUsuariosFiltrados(null, "ACTIVO").size());
        assertEquals(2, adminService.obtenerUsuariosFiltrados("  ", "INACTIVO").size());
        assertEquals(0, adminService.obtenerUsuariosFiltrados("VENDEDOR", "BLOQUEADO").size());
        assertEquals(0, adminService.obtenerUsuariosFiltrados("ROL_DESCONOCIDO", "ACTIVO").size());
        assertEquals(0, adminService.obtenerUsuariosFiltrados("CLIENTE", "UNKNOWN").size());
        assertEquals(1, adminService.obtenerUsuariosFiltrados("CLIENTE", null).size());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.activarUsuario("missing"));
        assertThrows(UserNotFoundException.class, () -> adminService.desactivarUsuario("missing"));
        assertThrows(UserNotFoundException.class, () -> adminService.bloquearUsuario("missing"));
        assertThrows(UserNotFoundException.class, () -> adminService.desbloquearUsuario("missing"));
    }

    @Test
    public void ComprobarErrorPorEmailDuplicado() {
        AltaAdminDTO request = new AltaAdminDTO(
                "12345678Z", "Carlos", "Gómez Pérez", "carlos.duplicado@esibuy.com",
                "Admin123456!#", "Admin123456!#", "Ciudad Real", null);

        when(userRepository.existsByEmailIgnoreCase("carlos.duplicado@esibuy.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> adminService.crearAdministrador(request));
    }
}
