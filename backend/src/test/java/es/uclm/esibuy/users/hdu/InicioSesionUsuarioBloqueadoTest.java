package es.uclm.esibuy.users.hdu;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import es.uclm.esibuy.users.service.UserService;
import es.uclm.esibuy.common.exception.InvalidCredentialsException;

@ExtendWith(MockitoExtension.class)
class InicioSesionUsuarioBloqueadoTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void unaCuentaBloqueadaNoPuedeIniciarSesion() {
        // Arrange
        User usuario = new User(
                "usuario@test.com",
                "Juan",
                "Pérez",
                UserRole.CLIENT
        );
        usuario.setActive(false);
        usuario.setPasswordHash("hash");

        LoginRequest request = new LoginRequest();
        request.setEmail("usuario@test.com");
        request.setPassword("123456");

        when(userRepository.findByEmailIgnoreCase("usuario@test.com"))
                .thenReturn(Optional.of(usuario));
        //when(passwordEncoder.matches("123456", "hash"))
               // .thenReturn(true);

        UserService userService = new UserService(userRepository, passwordEncoder);

        // Act + Assert
        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );
    }
}