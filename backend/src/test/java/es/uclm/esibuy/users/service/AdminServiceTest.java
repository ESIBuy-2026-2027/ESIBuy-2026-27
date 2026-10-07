package es.uclm.esibuy.users.service;

import java.util.List;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.users.dto.AltaAdminDTO;
import es.uclm.esibuy.users.dto.UserAdminDTO;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    public void ComprobarErrorPorEmailDuplicado() {
        AltaAdminDTO request = new AltaAdminDTO(
                "12345678Z", "Carlos", "Gómez Pérez", "carlos.duplicado@esibuy.com",
                "Admin123456!#", "Admin123456!#", "Ciudad Real", null);

        when(userRepository.existsByEmailIgnoreCase("carlos.duplicado@esibuy.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> adminService.crearAdministrador(request));
    }
}
