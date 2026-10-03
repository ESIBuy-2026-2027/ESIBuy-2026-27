package es.uclm.esibuy.users.service;

import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public AdminUserServiceImpl(UserRepository userRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public User obtenerPorId(String id) {
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    @Override
    public User actualizar(String id, UpdateUserRequestDTO request) {
        User user = obtenerPorId(id);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(blankToNull(request.getPhone()));
        return userRepository.save(user);
    }

    @Override
    public void eliminar(String id, String authenticatedUserId) {
        if (id == null || !id.matches("[a-fA-F0-9]{24}")) {
            throw new IllegalStateException("El identificador del usuario no es válido");
        }
        if (id.equalsIgnoreCase(authenticatedUserId)) {
            throw new IllegalStateException("No puedes eliminar tu propia cuenta");
        }

        User user = obtenerPorId(id);
        if (user.getRole() == UserRole.SELLER && productRepository.countBySellerId(id) > 0) {
            throw new UserDeletionConflictException();
        }

        userRepository.delete(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}