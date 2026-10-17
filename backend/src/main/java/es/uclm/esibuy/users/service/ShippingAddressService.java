package es.uclm.esibuy.users.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import es.uclm.esibuy.common.exception.AddressLimitExceededException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.ShippingAddressRequest;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.ShippingAddress;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class ShippingAddressService {

    public static final int MAX_ADDRESSES = 10;

    private final UserRepository userRepository;

    public ShippingAddressService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<ShippingAddress> list(String userId) {
        User user = findUser(userId);
        ClientProfile profile = user.getClientProfile();
        return profile == null ? List.of() : profile.getAddresses();
    }

    public ShippingAddress add(String userId, ShippingAddressRequest request) {
        User user = findUser(userId);

        ClientProfile profile = user.getClientProfile();
        if (profile == null) {
            profile = new ClientProfile();
            user.setClientProfile(profile);
        }

        List<ShippingAddress> addresses = new ArrayList<>(profile.getAddresses());
        if (addresses.size() >= MAX_ADDRESSES) {
            throw new AddressLimitExceededException(
                    "Has alcanzado el máximo de " + MAX_ADDRESSES + " direcciones de envío");
        }

        // La primera dirección es siempre la predeterminada; solo puede haber una.
        boolean makeDefault = request.defaultAddress() || addresses.isEmpty();
        if (makeDefault) {
            addresses.forEach(address -> address.setDefaultAddress(false));
        }

        ShippingAddress created = new ShippingAddress(
                UUID.randomUUID().toString(),
                request.recipientName().trim(),
                request.street().trim(),
                blankToNull(request.additionalInfo()),
                request.postalCode().trim(),
                request.city().trim(),
                request.province().trim(),
                blankToNull(request.phone()),
                makeDefault);

        addresses.add(created);
        profile.setAddresses(addresses);
        userRepository.save(user);
        return created;
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
