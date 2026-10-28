package es.uclm.esibuy.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
        private static final String MESSAGE = "message";
        private static final String CORRELATION_ID = "correlationId";

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception,
                        HttpServletRequest request) {
                Map<String, String> errors = new LinkedHashMap<>();
                exception.getBindingResult().getFieldErrors()
                                .forEach(error -> addError(errors, error.getField(), error.getDefaultMessage()));
                exception.getBindingResult().getGlobalErrors()
                                .forEach(error -> {
                                        String field = error.getCode() != null
                                                        && error.getCode().equals("AssertTrue")
                                                                        ? "passwordsMatch"
                                                                        : error.getObjectName();

                                        errors.put(field, error.getDefaultMessage());
                                });
                return ResponseEntity.badRequest()
                                .body(Map.of(MESSAGE, "La solicitud contiene errores", "errors", errors,
                                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<Map<String, String>> handleMalformedJson(HttpMessageNotReadableException exception,
                        HttpServletRequest request) {
                return ResponseEntity.badRequest().body(Map.of(MESSAGE, "El formato de la solicitud no es válido",
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException exception) {
                Map<String, String> errors = exception.getConstraintViolations().stream()
                                .collect(Collectors.toMap(violation -> violation.getPropertyPath().toString(),
                                                ConstraintViolation::getMessage, (first, ignored) -> first,
                                                LinkedHashMap::new));
                return ResponseEntity.badRequest()
                                .body(Map.of(MESSAGE, "La solicitud contiene errores", "errors", errors));
        }

        @ExceptionHandler(CategoryNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleCategoryNotFound(CategoryNotFoundException exception) {
                return ResponseEntity.badRequest().body(Map.of(MESSAGE, exception.getMessage()));
        }

        @ExceptionHandler(DuplicateCategoryException.class)
        public ResponseEntity<Map<String, String>> handleDuplicateCategory(DuplicateCategoryException exception,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(CategoryInUseException.class)
        public ResponseEntity<Map<String, String>> handleCategoryInUse(CategoryInUseException exception,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(IncidentNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleIncidentNotFound(IncidentNotFoundException exception,
                        HttpServletRequest request) {
                String id = correlationId(request);
                LOGGER.warn("Incidencia solicitada no encontrada. correlationId={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, id));
        }

        @ExceptionHandler(CategoryIdNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleCategoryIdNotFound(CategoryIdNotFoundException exception,
                        HttpServletRequest request) {
                String id = correlationId(request);
                LOGGER.warn("Categoría solicitada no encontrada. correlationId={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, id));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(MESSAGE, exception.getMessage()));
        }

        @ExceptionHandler(DuplicateUserException.class)
        public ResponseEntity<Map<String, String>> handleDuplicateUser(DuplicateUserException exception,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(UserDeletionConflictException.class)
        public ResponseEntity<Map<String, String>> handleUserDeletionConflict(UserDeletionConflictException exception,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception,
                        HttpServletRequest request) {
                String id = correlationId(request);
                if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Usuario solicitado no encontrado. correlationId={}", id);
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE, "Usuario no encontrado",
                                CORRELATION_ID, id));
        }

        @ExceptionHandler(ProductNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleProductNotFound(ProductNotFoundException exception,
                        HttpServletRequest request) {
                String id = correlationId(request);
                LOGGER.warn("Producto solicitado no encontrado. correlationId={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, id));
        }

        @ExceptionHandler(PedidoNotFoundException.class)
        public ResponseEntity<Map<String, String>> handlePedidoNotFound(PedidoNotFoundException exception,
                        HttpServletRequest request) {
                String id = correlationId(request);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, id));
        }

        @ExceptionHandler(ProductoNoDisponibleException.class)
        public ResponseEntity<Map<String, String>> handleCartConflict(IllegalStateException exception,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(StockInsuficienteException.class)
        public ResponseEntity<Map<String, String>> handleInsufficientStock(StockInsuficienteException exception,
                        HttpServletRequest request) {
                return ResponseEntity.badRequest().body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(PagoFallidoException.class)
        public ResponseEntity<Map<String, String>> handlePaymentFailure(
                        PagoFallidoException exception, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                                MESSAGE, exception.getMessage(), CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(RecursoCheckoutNoEncontradoException.class)
        public ResponseEntity<Map<String, String>> handleCheckoutResourceNotFound(
                        RecursoCheckoutNoEncontradoException exception, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                MESSAGE, exception.getMessage(), CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception,
                        HttpServletRequest request) {
                return ResponseEntity.badRequest().body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception,
                        HttpServletRequest request) {
                String id = correlationId(request);
                if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Solicitud rechazada por estado no válido. correlationId={}", id);
                }
                return ResponseEntity.badRequest().body(Map.of(MESSAGE, exception.getMessage(),
                                CORRELATION_ID, id));
        }

        @ExceptionHandler(AddressNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleAddressNotFound(
                        AddressNotFoundException exception, HttpServletRequest request) {
                String id = correlationId(request);
                LOGGER.warn("Dirección solicitada no encontrada. correlationId={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                                MESSAGE, exception.getMessage(), CORRELATION_ID, id));
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<Map<String, String>> handleResponseStatusException(
                        ResponseStatusException exception, HttpServletRequest request) {
                String id = correlationId(request);
                HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
                String message = exception.getReason();
                if (message == null || message.isBlank()) {
                        message = status == null ? "La solicitud no se ha podido procesar" : status.getReasonPhrase();
                }
                return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                                MESSAGE, message, CORRELATION_ID, id));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception, HttpServletRequest request) {
                String correlationId = correlationId(request);
                LOGGER.error("Error inesperado procesando solicitud. correlationId={}", correlationId, exception);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                                MESSAGE, "Se produjo un error interno. Consulte el identificador de correlación.",
                                CORRELATION_ID, correlationId));
        }

        private String correlationId(HttpServletRequest request) {
                Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
                return correlationId == null ? "" : correlationId.toString();
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException exception) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(MESSAGE, exception.getMessage()));
        }

        @ExceptionHandler(TooManyRequestsException.class)
        public ResponseEntity<Map<String, String>> handleTooManyRequests(TooManyRequestsException exception,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .header("Retry-After", Long.toString(exception.getRetryAfterSeconds()))
                                .body(Map.of(
                                                MESSAGE, exception.getMessage(),
                                                CORRELATION_ID, correlationId(request)));
        }

        private void addError(Map<String, String> errors, String field, String message) {
                errors.merge(field, message,
                                (existing, next) -> existing.equals(next) ? existing : existing + "; " + next);
        }

        @ExceptionHandler(AddressLimitExceededException.class)
        public ResponseEntity<Map<String, String>> handleAddressLimit(
                        AddressLimitExceededException exception, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                                MESSAGE, exception.getMessage(), CORRELATION_ID, correlationId(request)));
        }

        @ExceptionHandler(InvalidOtpException.class)
        public ResponseEntity<Map<String, String>> handleInvalidOtp(
                        InvalidOtpException exception) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(MESSAGE, exception.getMessage()));
        }
}