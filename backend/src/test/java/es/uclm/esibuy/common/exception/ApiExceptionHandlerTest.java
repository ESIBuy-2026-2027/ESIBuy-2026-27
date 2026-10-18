package es.uclm.esibuy.common.exception;

import es.uclm.esibuy.common.filter.CorrelationIdFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void addressNotFoundReturnsNotFound() {
        MockHttpServletRequest request = requestWithCorrelationId("test-address-404");

        ResponseEntity<Map<String, String>> response = handler.handleAddressNotFound(new AddressNotFoundException(),
                request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Dirección no encontrada", response.getBody().get("message"));
        assertEquals("test-address-404", response.getBody().get("correlationId"));
    }

    @Test
    void responseStatusExceptionPreservesClientErrorStatusAndReason() {
        MockHttpServletRequest request = requestWithCorrelationId("test-payment-409");

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.CONFLICT, "Payment method conflict"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Payment method conflict", response.getBody().get("message"));
        assertEquals("test-payment-409", response.getBody().get("correlationId"));
    }

    @Test
    void responseStatusExceptionPreservesBadRequestStatus() {
        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment method request"),
                new MockHttpServletRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid payment method request", response.getBody().get("message"));
    }

    @Test
    void responseStatusExceptionWithoutReasonStillHasUsefulMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND), new MockHttpServletRequest());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().get("message").length() > 0);
    }

    private MockHttpServletRequest requestWithCorrelationId(String id) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE, id);
        return request;
    }
}
