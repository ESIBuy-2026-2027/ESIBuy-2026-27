package es.uclm.esibuy.cart.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCartItemQuantityDTOTest {

    @Test
    void shouldAcceptZeroAndPositiveQuantities() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(new UpdateCartItemQuantityDTO(0)).isEmpty());
            assertTrue(validator.validate(new UpdateCartItemQuantityDTO(2)).isEmpty());
        }
    }

    @Test
    void shouldRejectNegativeAndMissingQuantities() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<UpdateCartItemQuantityDTO>> violations = validator
                    .validate(new UpdateCartItemQuantityDTO(-1));

            assertEquals(1, violations.size());
            assertEquals("quantity", violations.iterator().next().getPropertyPath().toString());
            assertEquals(1, validator.validate(new UpdateCartItemQuantityDTO(null)).size());
        }
    }
}