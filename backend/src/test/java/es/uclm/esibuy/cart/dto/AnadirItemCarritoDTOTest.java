package es.uclm.esibuy.cart.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnadirItemCarritoDTOTest {

    @Test
    void shouldAcceptAProductIdAndPositiveQuantity() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(new AnadirItemCarritoDTO("producto-1", 1)).isEmpty());
        }
    }

    @Test
    void shouldRejectMissingProductId() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<AnadirItemCarritoDTO>> violations =
                    validator.validate(new AnadirItemCarritoDTO(" ", 1));

            assertEquals(1, violations.size());
            assertEquals("productoId", violations.iterator().next().getPropertyPath().toString());
        }
    }

    @Test
    void shouldRejectMissingOrNonPositiveQuantity() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertFalse(validator.validate(new AnadirItemCarritoDTO("producto-1", null)).isEmpty());
            assertFalse(validator.validate(new AnadirItemCarritoDTO("producto-1", 0)).isEmpty());
            assertFalse(validator.validate(new AnadirItemCarritoDTO("producto-1", -1)).isEmpty());
        }
    }
}
