package es.uclm.esibuy.users.hdu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.users.service.AdminService;

@ExtendWith(MockitoExtension.class)
class BloqueoDesbloqueoUsuarioTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminService adminService;

    @Test
    void unAdministradorPuedeBloquearUnaCuentaActiva() {
        // Arrange
        //MockitoAnnotations.openMocks(this);

        User usuario = new User(
                "usuario@test.com",
                "Juan",
                "Pérez",
                UserRole.CLIENT
        );

        usuario.setActive(true);

        when(userRepository.findById("123"))
                .thenReturn(Optional.of(usuario));

        adminService = new AdminService(userRepository, passwordEncoder);

        // Act
        adminService.bloquearUsuario("123");

        // Assert
        assertFalse(usuario.isActive());
    }
}
