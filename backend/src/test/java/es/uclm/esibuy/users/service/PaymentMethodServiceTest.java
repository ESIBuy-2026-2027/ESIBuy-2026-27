
package es.uclm.esibuy.users.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import es.uclm.esibuy.users.dto.PaymentMethodRequest;
import es.uclm.esibuy.users.dto.PaymentMethodResponse;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.PaymentMethodType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    private static final String USER_ID = "user-1";

    @Mock
    private UserRepository userRepository;

    private PaymentMethodService service;
    private User client;

    @BeforeEach
    void setUp() {
        service = new PaymentMethodService(userRepository);
        client = new User(
                "ana@example.com", "Ana", "García", UserRole.CLIENT);
        client.setClientProfile(new ClientProfile());
    }

    private void givenClientExists() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(client));
    }

    private PaymentMethodRequest cardRequest(boolean isDefault) {
        return new PaymentMethodRequest(
                PaymentMethodType.CARD,
                " Visa ",
                " Ana García ",
                " Visa ",
                "1234",
                12,
                2099,
                null,
                null,
                isDefault);
    }

    private PaymentMethod card(String id, boolean isDefault) {
        return PaymentMethod.card(
                id,
                "Visa",
                "Ana García",
                "Visa",
                "1234",
                12,
                2099,
                isDefault);
    }

    private ResponseStatusException expectStatus(
            HttpStatus expected, Runnable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, exception.getStatusCode());
        return exception;
    }

    @Test
    void shouldAddFirstMethodAsDefault() {
        givenClientExists();

        PaymentMethodResponse created = service.add(USER_ID, cardRequest(false));

        assertNotNull(created.id());
        assertTrue(created.defaultMethod());
        assertEquals(PaymentMethodType.CARD, created.type());
        assertEquals("Ana García", created.cardholderName());
        assertEquals(1,
                client.getClientProfile().getPaymentMethods().size());
        verify(userRepository).save(client);
    }

    @Test
    void shouldReplaceExistingDefaultWhenAddingAnotherDefault() {
        givenClientExists();
        client.getClientProfile().setPaymentMethods(
                new ArrayList<>(List.of(card("p1", true))));

        PaymentMethodResponse created = service.add(USER_ID, cardRequest(true));

        List<PaymentMethod> methods = client.getClientProfile().getPaymentMethods();

        assertTrue(created.defaultMethod());
        assertFalse(methods.get(0).isDefaultMethod());
        assertTrue(methods.get(1).isDefaultMethod());
    }

    @Test
    void shouldKeepExistingDefaultWhenAddingNonDefaultMethod() {
        givenClientExists();
        client.getClientProfile().setPaymentMethods(
                new ArrayList<>(List.of(card("p1", true))));

        service.add(USER_ID, cardRequest(false));

        List<PaymentMethod> methods = client.getClientProfile().getPaymentMethods();

        assertTrue(methods.get(0).isDefaultMethod());
        assertFalse(methods.get(1).isDefaultMethod());
    }

    @Test
    void shouldCreateClientProfileWhenItDoesNotExist() {
        givenClientExists();
        client.setClientProfile(null);

        service.add(USER_ID, cardRequest(false));

        assertNotNull(client.getClientProfile());
        assertEquals(1,
                client.getClientProfile().getPaymentMethods().size());
    }

    @Test
    void shouldListEmptyWhenClientHasNoProfile() {
        givenClientExists();
        client.setClientProfile(null);

        assertTrue(service.list(USER_ID).isEmpty());
    }

    @Test
    void shouldSetRequestedMethodAsDefault() {
        givenClientExists();
        client.getClientProfile().setPaymentMethods(
                new ArrayList<>(List.of(
                        card("p1", true),
                        card("p2", false))));

        PaymentMethodResponse result = service.setDefault(USER_ID, "p2");

        assertEquals("p2", result.id());
        assertTrue(result.defaultMethod());
        assertFalse(client.getClientProfile()
                .getPaymentMethods().get(0).isDefaultMethod());
    }

    @Test
    void shouldUpdateMethodWithoutChangingItsId() {
        givenClientExists();
        client.getClientProfile().setPaymentMethods(
                new ArrayList<>(List.of(card("p1", true))));

        PaymentMethodResponse result = service.update(USER_ID, "p1", cardRequest(false));

        assertEquals("p1", result.id());
        assertTrue(result.defaultMethod());
        assertEquals("1234", result.lastFourDigits());
    }

    @Test
    void shouldDeleteDefaultAndAssignAnotherMethodAsDefault() {
        givenClientExists();
        client.getClientProfile().setPaymentMethods(
                new ArrayList<>(List.of(
                        card("p1", true),
                        card("p2", false))));

        service.delete(USER_ID, "p1");

        List<PaymentMethod> methods = client.getClientProfile().getPaymentMethods();

        assertEquals(1, methods.size());
        assertEquals("p2", methods.get(0).getId());
        assertTrue(methods.get(0).isDefaultMethod());
    }

    @Test
    void shouldRejectMoreThanTenMethods() {
        givenClientExists();

        List<PaymentMethod> methods = new ArrayList<>();
        for (int i = 0; i < PaymentMethodService.MAX_PAYMENT_METHODS; i++) {
            methods.add(card("p" + i, i == 0));
        }
        client.getClientProfile().setPaymentMethods(methods);

        expectStatus(
                HttpStatus.CONFLICT,
                () -> service.add(USER_ID, cardRequest(false)));

        verify(userRepository, never()).save(client);
    }

    @Test
    void shouldRejectExpiredCard() {
        givenClientExists();

        YearMonth previousMonth = YearMonth.now().minusMonths(1);

        PaymentMethodRequest expired = new PaymentMethodRequest(
                PaymentMethodType.CARD,
                "Visa",
                "Ana García",
                "Visa",
                "1234",
                previousMonth.getMonthValue(),
                previousMonth.getYear(),
                null,
                null,
                false);

        expectStatus(
                HttpStatus.BAD_REQUEST,
                () -> service.add(USER_ID, expired));

        verify(userRepository, never()).save(client);
    }

    @Test
    void shouldRejectCardWithMissingRequiredData() {
        givenClientExists();

        PaymentMethodRequest invalid = new PaymentMethodRequest(
                PaymentMethodType.CARD,
                "Visa",
                " ",
                "Visa",
                "1234",
                12,
                2099,
                null,
                null,
                false);

        expectStatus(
                HttpStatus.BAD_REQUEST,
                () -> service.add(USER_ID, invalid));
    }

    @Test
    void shouldRejectBizumWithInvalidPhone() {
        givenClientExists();

        PaymentMethodRequest invalid = new PaymentMethodRequest(
                PaymentMethodType.BIZUM,
                "Bizum",
                null,
                null,
                null,
                null,
                null,
                null,
                "123",
                false);

        expectStatus(
                HttpStatus.BAD_REQUEST,
                () -> service.add(USER_ID, invalid));
    }

    @Test
    void shouldRejectPayPalWithoutEmail() {
        givenClientExists();

        PaymentMethodRequest invalid = new PaymentMethodRequest(
                PaymentMethodType.PAYPAL,
                "PayPal",
                null,
                null,
                null,
                null,
                null,
                " ",
                null,
                false);

        expectStatus(
                HttpStatus.BAD_REQUEST,
                () -> service.add(USER_ID, invalid));
    }

    @Test
    void shouldReturnNotFoundForMissingPaymentMethod() {
        givenClientExists();

        expectStatus(
                HttpStatus.NOT_FOUND,
                () -> service.setDefault(USER_ID, "missing"));

        expectStatus(
                HttpStatus.NOT_FOUND,
                () -> service.update(
                        USER_ID, "missing", cardRequest(false)));

        expectStatus(
                HttpStatus.NOT_FOUND,
                () -> service.delete(USER_ID, "missing"));
    }

    @Test
    void shouldReturnNotFoundForMissingUser() {
        when(userRepository.findById("missing"))
                .thenReturn(Optional.empty());

        expectStatus(
                HttpStatus.NOT_FOUND,
                () -> service.list("missing"));
    }

    @Test
    void shouldForbidNonClientUsers() {
        User seller = new User(
                "seller@example.com",
                "Luis",
                "Martín",
                UserRole.SELLER);

        when(userRepository.findById("seller-1"))
                .thenReturn(Optional.of(seller));

        expectStatus(
                HttpStatus.FORBIDDEN,
                () -> service.list("seller-1"));
    }
}
