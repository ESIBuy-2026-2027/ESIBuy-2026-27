package es.uclm.esibuy.users.hdu;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import es.uclm.esibuy.common.exception.InvalidCredentialsException;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.wishlist.repository.WishlistRepository;
import es.uclm.esibuy.security.repository.EmailOtpChallengeRepository;
import es.uclm.esibuy.auth.passwordreset.repository.PasswordResetTokenRepository;
import es.uclm.esibuy.security.repository.LoginAttemptRepository;

@ExtendWith(MockitoExtension.class)
class InicioSesionUsuarioDesbloqueadoTest {
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private WishlistRepository wishlistRepository;
    @Mock private EmailOtpChallengeRepository emailOtpChallengeRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private LoginAttemptRepository loginAttemptRepository;

    private UserService createService() {
        return new UserService(userRepository, passwordEncoder, productRepository,
                orderRepository, cartRepository, wishlistRepository,
                emailOtpChallengeRepository, passwordResetTokenRepository, loginAttemptRepository);
    }

    private LoginRequest request(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void unaCuentaActivaYDesbloqueadaPuedeIniciarSesion() {
        User user = new User("usuario@test.com", "Juan", "Pérez", UserRole.CLIENT);
        user.setActive(true);
        user.setBlocked(false);
        user.setPasswordHash("hash");
        when(userRepository.findByEmailIgnoreCase("usuario@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertEquals(user, createService().login(request("usuario@test.com", "123456")));
    }

    @Test
    void normalizaElEmailAntesDeBuscarLaCuenta() {
        User user = new User("usuario@test.com", "Juan", "Pérez", UserRole.CLIENT);
        user.setActive(true);
        user.setBlocked(false);
        user.setPasswordHash("hash");
        when(userRepository.findByEmailIgnoreCase("usuario@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertEquals(user, createService().login(request(" USUARIO@Test.com ", "123456")));
    }
}
