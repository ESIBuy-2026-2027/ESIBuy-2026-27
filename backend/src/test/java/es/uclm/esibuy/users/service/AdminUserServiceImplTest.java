package es.uclm.esibuy.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private AdminUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserServiceImpl(userRepository);
    }

    @Test
    void updatesOnlyWhitelistedProperties() {
        User existing = new User("cliente@example.com", "Old", "Name", UserRole.CLIENT);
        existing.setPhone("600111222");
        existing.setPasswordHash("encoded-secret");
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setFirstName(" New ");
        request.setLastName(" Name ");
        request.setPhone("600111222");
        request.setProfileImage("assets/images/avatars/av_client_2.jpeg");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User result = service.actualizar("user-1", request);

        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals("600111222", result.getPhone());
        assertEquals("assets/images/avatars/av_client_2.jpeg", result.getProfileImage());
        assertEquals(UserRole.CLIENT, result.getRole());
        assertEquals("encoded-secret", result.getPasswordHash());
        assertEquals("cliente@example.com", result.getEmail());
        verify(userRepository).save(existing);
    }

    @Test
    void failsWhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.obtenerPorId("missing"));
    }
}