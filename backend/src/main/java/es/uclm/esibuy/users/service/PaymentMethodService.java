package es.uclm.esibuy.users.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import es.uclm.esibuy.users.dto.PaymentMethodRequest;
import es.uclm.esibuy.users.dto.PaymentMethodResponse;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class PaymentMethodService {

    public static final int MAX_PAYMENT_METHODS = 10;

    private final UserRepository userRepository;

    public PaymentMethodService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<PaymentMethodResponse> list(String userId) {
        ClientProfile profile = getClientProfile(userId);

        if (profile == null) {
            return List.of();
        }

        return profile.getPaymentMethods().stream()
                .map(PaymentMethodResponse::from)
                .toList();
    }

    public PaymentMethodResponse add(String userId, PaymentMethodRequest request) {
        User user = findClient(userId);
        ClientProfile profile = user.getClientProfile();

        if (profile == null) {
            profile = new ClientProfile();
            user.setClientProfile(profile);
        }

        List<PaymentMethod> methods = new ArrayList<>(profile.getPaymentMethods());

        if (methods.size() >= MAX_PAYMENT_METHODS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Has alcanzado el máximo de " + MAX_PAYMENT_METHODS + " métodos de pago");
        }

        validateRequest(request);

        boolean makeDefault = request.defaultMethod() || methods.isEmpty();

        if (makeDefault) {
            methods.forEach(method -> method.setDefaultMethod(false));
        }

        PaymentMethod created = createPaymentMethod(request, makeDefault);

        methods.add(created);
        profile.setPaymentMethods(methods);
        userRepository.save(user);

        return PaymentMethodResponse.from(created);
    }

    public PaymentMethodResponse setDefault(String userId, String paymentMethodId) {
        User user = findClient(userId);
        ClientProfile profile = user.getClientProfile();

        if (profile == null) {
            throw paymentMethodNotFound();
        }

        List<PaymentMethod> methods = new ArrayList<>(profile.getPaymentMethods());

        PaymentMethod target = methods.stream()
                .filter(method -> method.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(this::paymentMethodNotFound);

        methods.forEach(method -> method.setDefaultMethod(false));
        target.setDefaultMethod(true);

        profile.setPaymentMethods(methods);
        userRepository.save(user);

        return PaymentMethodResponse.from(target);
    }

    public PaymentMethodResponse update(
            String userId,
            String paymentMethodId,
            PaymentMethodRequest request) {

        User user = findClient(userId);
        ClientProfile profile = user.getClientProfile();

        if (profile == null) {
            throw paymentMethodNotFound();
        }

        validateRequest(request);

        List<PaymentMethod> methods = new ArrayList<>(profile.getPaymentMethods());

        int targetIndex = -1;

        for (int i = 0; i < methods.size(); i++) {
            if (methods.get(i).getId().equals(paymentMethodId)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            throw paymentMethodNotFound();
        }

        PaymentMethod current = methods.get(targetIndex);

        // Si ya era el método predeterminado, conservar ese estado.
        boolean makeDefault = request.defaultMethod() || current.isDefaultMethod();

        if (makeDefault) {
            methods.forEach(method -> method.setDefaultMethod(false));
        }

        // Mantener el mismo ID para que siga siendo el mismo método.
        PaymentMethod updated = createPaymentMethod(
                current.getId(),
                request,
                makeDefault);

        // createPaymentMethod genera un ID nuevo; restauramos el original.
        // Para evitar esto, el método de creación debe aceptar el ID.
        methods.set(targetIndex, updated);

        profile.setPaymentMethods(methods);
        userRepository.save(user);

        return PaymentMethodResponse.from(updated);
    }

    public void delete(String userId, String paymentMethodId) {
        User user = findClient(userId);
        ClientProfile profile = user.getClientProfile();

        if (profile == null) {
            throw paymentMethodNotFound();
        }

        List<PaymentMethod> methods = new ArrayList<>(profile.getPaymentMethods());

        PaymentMethod target = methods.stream()
                .filter(method -> method.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(this::paymentMethodNotFound);

        boolean wasDefault = target.isDefaultMethod();

        methods.remove(target);

        if (wasDefault && !methods.isEmpty()) {
            methods.forEach(method -> method.setDefaultMethod(false));
            methods.get(0).setDefaultMethod(true);
        }

        profile.setPaymentMethods(methods);
        userRepository.save(user);
    }

    private PaymentMethod createPaymentMethod(
            PaymentMethodRequest request,
            boolean makeDefault) {

        return createPaymentMethod(
                UUID.randomUUID().toString(),
                request,
                makeDefault);
    }

    private PaymentMethod createPaymentMethod(
            String id,
            PaymentMethodRequest request,
            boolean makeDefault) {

        String label = request.label() == null
                ? ""
                : request.label().trim();

        return switch (request.type()) {
            case CARD -> PaymentMethod.card(
                    id,
                    label,
                    request.cardholderName().trim(),
                    request.cardBrand().trim(),
                    request.lastFourDigits(),
                    request.expirationMonth(),
                    request.expirationYear(),
                    makeDefault);
            case PAYPAL -> PaymentMethod.paypal(
                    id,
                    label,
                    request.paypalEmail().trim(),
                    makeDefault);
            case BIZUM -> PaymentMethod.bizum(
                    id,
                    label,
                    request.bizumPhone().trim(),
                    makeDefault);
        };
    }

    private void validateRequest(PaymentMethodRequest request) {
        if (request.type() == null) {
            throw badRequest("El tipo de método de pago es obligatorio");
        }

        switch (request.type()) {
            case CARD -> validateCard(request);
            case PAYPAL -> validatePayPal(request);
            case BIZUM -> validateBizum(request);
        }
    }

    private void validateCard(PaymentMethodRequest request) {
        if (isBlank(request.cardholderName())) {
            throw badRequest("El titular de la tarjeta es obligatorio");
        }

        if (isBlank(request.cardBrand())) {
            throw badRequest("La marca de la tarjeta es obligatoria");
        }

        if (request.lastFourDigits() == null
                || !request.lastFourDigits().matches("\\d{4}")) {
            throw badRequest("Debes indicar los últimos cuatro dígitos de la tarjeta");
        }

        if (request.expirationMonth() == null
                || request.expirationYear() == null) {
            throw badRequest("El mes y el año de caducidad son obligatorios");
        }

        YearMonth expiration = YearMonth.of(
                request.expirationYear(),
                request.expirationMonth());

        if (expiration.isBefore(YearMonth.now())) {
            throw badRequest("La tarjeta está caducada");
        }

        if (request.expirationMonth() < 1 || request.expirationMonth() > 12) {
            throw badRequest("El mes de caducidad debe estar entre 1 y 12");
        }

        if (request.expirationYear() < 2000 || request.expirationYear() > 9999) {
            throw badRequest("El año de caducidad no es válido");
        }
    }

    private void validatePayPal(PaymentMethodRequest request) {
        if (isBlank(request.paypalEmail())) {
            throw badRequest("El email de PayPal es obligatorio");
        }
    }

    private void validateBizum(PaymentMethodRequest request) {
        if (request.bizumPhone() == null
                || !request.bizumPhone().matches("\\d{9}")) {
            throw badRequest("El teléfono de Bizum debe contener 9 dígitos");
        }
    }

    private ClientProfile getClientProfile(String userId) {
        return findClient(userId).getClientProfile();
    }

    private User findClient(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"));

        if (user.getRole() != UserRole.CLIENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los clientes pueden gestionar métodos de pago");
        }

        return user;
    }

    private ResponseStatusException paymentMethodNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Método de pago no encontrado");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}