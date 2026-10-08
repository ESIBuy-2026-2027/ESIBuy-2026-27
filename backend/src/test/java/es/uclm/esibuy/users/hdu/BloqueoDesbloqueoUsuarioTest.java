package es.uclm.esibuy.users.hdu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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


import es.uclm.esibuy.common.exception.UserNotFoundException;




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

    @Test
    void noSePuedeBloquearUnUsuarioInexistente() {
        // Arrange
        when(userRepository.findById("999"))
                .thenReturn(Optional.empty());

        adminService = new AdminService(userRepository, passwordEncoder);

        // Act + Assert
        assertThrows(
                UserNotFoundException.class,
                () -> adminService.bloquearUsuario("999")
        );
    }


        @Test
    void unAdministradorPuedeDesbloquearUnaCuentaBloqueada() {
        // Arrange
        User usuario = new User(
                "usuario@test.com",
                "Juan",
                "Pérez",
                UserRole.CLIENT
        );
        usuario.setActive(false);

        when(userRepository.findById("123"))
                .thenReturn(Optional.of(usuario));

        adminService = new AdminService(userRepository, passwordEncoder);

        // Act
        adminService.desbloquearUsuario("123");

        // Assert
        assertTrue(usuario.isActive());
    }


    @Test
    void noSePuedeDesbloquearUnUsuarioInexistente() {
        // Arrange
        when(userRepository.findById("999"))
                .thenReturn(Optional.empty());

        adminService = new AdminService(userRepository, passwordEncoder);

        // Act + Assert
        assertThrows(
                UserNotFoundException.class,
                () -> adminService.desbloquearUsuario("999")
        );
    }
}
