package es.uclm.esibuy.cart.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActualizarCantidadItemDTOTest {

    @Test
    void shouldAcceptZeroAndPositiveQuantities() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(new ActualizarCantidadItemDTO(0)).isEmpty());
            assertTrue(validator.validate(new ActualizarCantidadItemDTO(2)).isEmpty());
        }
    }

    @Test
    void shouldRejectNegativeAndMissingQuantities() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<ActualizarCantidadItemDTO>> violations =
                    validator.validate(new ActualizarCantidadItemDTO(-1));
            assertEquals(1, violations.size());
            assertEquals("cantidad", violations.iterator().next().getPropertyPath().toString());
            assertEquals(1, validator.validate(new ActualizarCantidadItemDTO(null)).size());
        }
    }
}
