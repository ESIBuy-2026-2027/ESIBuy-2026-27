package es.uclm.esibuy.users.hdu;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.service.UserService;

@ExtendWith(MockitoExtension.class)
class InicioSesionUsuarioDesbloqueadoTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private ProductRepository productRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Test
        void unaCuentaDesbloqueadaPuedeIniciarSesionConCredencialesValidas() {
                // Arrange
                User usuario = new User(
                                "usuario@test.com",
                                "Juan",
                                "Pérez",
                                UserRole.CLIENT);
                usuario.setActive(true);
                usuario.setPasswordHash("hash");

                LoginRequest request = new LoginRequest();
                request.setEmail("usuario@test.com");
                request.setPassword("123456");

                when(userRepository.findByEmailIgnoreCase("usuario@test.com"))
                                .thenReturn(Optional.of(usuario));

                when(passwordEncoder.matches("123456", "hash"))
                                .thenReturn(true);

                UserService userService = new UserService(userRepository, passwordEncoder, productRepository);

                // Act
                User resultado = userService.login(request);

                // Assert
                assertEquals(usuario, resultado);
        }
}