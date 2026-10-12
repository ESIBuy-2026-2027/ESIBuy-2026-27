package es.uclm.esibuy.users.service;

import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    public AdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        user.setNationalId(blankToNull(request.getNationalId()));
        user.setProfileImage(blankToNull(request.getProfileImage()));

        if (user.getRole() != UserRole.ADMIN) {
            user.setPhone(blankToNull(request.getPhone()));
        }

        if (user.getRole() == UserRole.ADMIN && request.getHeadquarters() != null) {
            AdminProfile profile = user.getAdminProfile();
            if (profile == null) {
                profile = new AdminProfile();
                user.setAdminProfile(profile);
            }
            profile.setHeadquarters(blankToNull(request.getHeadquarters()));
        }

        if (user.getRole() == UserRole.SELLER) {
            SellerProfile profile = user.getSellerProfile();
            if (profile == null) {
                profile = new SellerProfile();
                user.setSellerProfile(profile);
            }
            if (request.getBusinessName() != null) {
                String newBusinessName = blankToNull(request.getBusinessName());
                if (newBusinessName != null && !newBusinessName.equalsIgnoreCase(profile.getBusinessName())
                        && userRepository.existsBySellerProfileBusinessNameIgnoreCase(newBusinessName)) {
                    throw new DuplicateUserException("Ya existe un vendedor con ese nombre comercial");
                }
                profile.setBusinessName(newBusinessName);
            }
            if (request.getDefaultCategory() != null) {
                profile.setDefaultCategory(blankToNull(request.getDefaultCategory()));
            }
        }

        if (user.getRole() == UserRole.CLIENT && request.getBirthDate() != null) {
            ClientProfile profile = user.getClientProfile();
            if (profile == null) {
                profile = new ClientProfile();
                user.setClientProfile(profile);
            }
            profile.setBirthDate(request.getBirthDate());
        }

        return userRepository.save(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}