package es.uclm.esibuy.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        exception.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.put(error.getObjectName(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                MESSAGE, "La solicitud contiene errores",
                "errors", errors,
                CORRELATION_ID, correlationId(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedRequest(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(Map.of(
                MESSAGE, "El formato de la solicitud no es válido",
                CORRELATION_ID, correlationId(request)));
    }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<Map<String, String>> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
                                MESSAGE, "El método HTTP no está permitido para este recurso",
                                CORRELATION_ID, correlationId(request)));
        }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException exception,
            HttpServletRequest request) {
        Map<String, String> errors = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage, (first, ignored) -> first, LinkedHashMap::new));
        return ResponseEntity.badRequest().body(Map.of(
                MESSAGE, "La solicitud contiene errores",
                "errors", errors,
                CORRELATION_ID, correlationId(request)));
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUser(DuplicateUserException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(MESSAGE, exception.getMessage(),
                CORRELATION_ID, correlationId(request)));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE, "Usuario no encontrado",
                CORRELATION_ID, correlationId(request)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(Map.of(MESSAGE, exception.getMessage(),
                CORRELATION_ID, correlationId(request)));
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
}
