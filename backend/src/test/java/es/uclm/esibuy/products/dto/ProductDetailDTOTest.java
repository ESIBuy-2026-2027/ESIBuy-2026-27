package es.uclm.esibuy.products.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import es.uclm.esibuy.products.model.Product;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductDetailDTOTest {

    private static final String DEFAULT_ID = "prod-002";
    private static final String DEFAULT_NAME = "Smart Watch";
    private static final String DEFAULT_DESCRIPTION = "Fitness tracking smartwatch";
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("149.99");
    private static final Integer DEFAULT_STOCK = 15;
    private static final String DEFAULT_CATEGORY = "Wearables";
    private static final String DEFAULT_SELLER_ID = "seller-456";
    private static final String DEFAULT_SELLER_NAME = "Gadget World";
    private static final Boolean DEFAULT_ACTIVE = true;

    private static final String PROPERTY_ID = "id";
    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_SELLER_ID = "sellerId";
    private static final String PROPERTY_ACTIVE = "active";

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    private ProductDetailDTO createValidDTO() {
        return new ProductDetailDTO(
                DEFAULT_ID,
                DEFAULT_NAME,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                DEFAULT_STOCK,
                DEFAULT_CATEGORY,
                DEFAULT_SELLER_ID,
                DEFAULT_SELLER_NAME,
                DEFAULT_ACTIVE);
    }

    @Test
    void shouldValidateValidProductDetailDTO() {
        ProductDetailDTO dto = createValidDTO();
        Set<ConstraintViolation<ProductDetailDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Valid DTO should produce zero violations");
        assertEquals(DEFAULT_ID, dto.id());
        assertEquals(DEFAULT_NAME, dto.name());
        assertEquals(DEFAULT_DESCRIPTION, dto.description());
        assertEquals(DEFAULT_PRICE, dto.price());
        assertEquals(DEFAULT_STOCK, dto.stock());
        assertEquals(DEFAULT_CATEGORY, dto.category());
        assertEquals(DEFAULT_SELLER_ID, dto.sellerId());
        assertEquals(DEFAULT_SELLER_NAME, dto.sellerName());
        assertEquals(DEFAULT_ACTIVE, dto.active());
    }

    @Test
    void shouldRejectBlankId() {
        ProductDetailDTO dto = new ProductDetailDTO(
                "   ",
                DEFAULT_NAME,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                DEFAULT_STOCK,
                DEFAULT_CATEGORY,
                DEFAULT_SELLER_ID,
                DEFAULT_SELLER_NAME,
                DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductDetailDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_ID)));
    }

    @Test
    void shouldRejectBlankName() {
        ProductDetailDTO dto = new ProductDetailDTO(
                DEFAULT_ID,
                "",
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                DEFAULT_STOCK,
                DEFAULT_CATEGORY,
                DEFAULT_SELLER_ID,
                DEFAULT_SELLER_NAME,
                DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductDetailDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_NAME)));
    }

    @Test
    void shouldRejectNameExceedingMaximumLength() {
        String longName = "B".repeat(151);
        ProductDetailDTO dto = new ProductDetailDTO(
                DEFAULT_ID,
                longName,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                DEFAULT_STOCK,
                DEFAULT_CATEGORY,
                DEFAULT_SELLER_ID,
                DEFAULT_SELLER_NAME,
                DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductDetailDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_NAME)));
    }

    @Test
    void shouldRejectNullOrZeroOrNegativePrice() {
        ProductDetailDTO nullPriceDTO = new ProductDetailDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, null, DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(nullPriceDTO).isEmpty());

        ProductDetailDTO zeroPriceDTO = new ProductDetailDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, BigDecimal.ZERO, DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(zeroPriceDTO).isEmpty());

        ProductDetailDTO negativePriceDTO = new ProductDetailDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, new BigDecimal("-10.00"), DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(negativePriceDTO).isEmpty());
    }

    @Test
    void shouldRejectNullOrNegativeStock() {
        ProductDetailDTO nullStockDTO = new ProductDetailDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, null,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(nullStockDTO).isEmpty());

        ProductDetailDTO negativeStockDTO = new ProductDetailDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, -1,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(negativeStockDTO).isEmpty());
    }

    @Test
    void shouldRejectBlankSellerId() {
        ProductDetailDTO dto = new ProductDetailDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, DEFAULT_STOCK,
                DEFAULT_CATEGORY, "  ", DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductDetailDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_SELLER_ID)));
    }

    @Test
    void shouldRejectNullActive() {
        ProductDetailDTO dto = new ProductDetailDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, null);

        Set<ConstraintViolation<ProductDetailDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_ACTIVE)));
    }

    @Test
    void shouldCreateFromProductWithCategoryAndSeller() {
        Product product = new Product(
                DEFAULT_NAME,
                List.of("cat-2"),
                DEFAULT_STOCK,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                null,
                null,
                "https://example.com/watch.jpg",
                true,
                DEFAULT_SELLER_ID);
        product.setId(DEFAULT_ID);

        ProductDetailDTO dto = ProductDetailDTO.from(product, DEFAULT_CATEGORY, DEFAULT_SELLER_NAME);

        assertEquals(DEFAULT_ID, dto.id());
        assertEquals(DEFAULT_NAME, dto.name());
        assertEquals(DEFAULT_DESCRIPTION, dto.description());
        assertEquals(DEFAULT_PRICE, dto.price());
        assertEquals(DEFAULT_STOCK, dto.stock());
        assertEquals(DEFAULT_CATEGORY, dto.category());
        assertEquals(DEFAULT_SELLER_ID, dto.sellerId());
        assertEquals(DEFAULT_SELLER_NAME, dto.sellerName());
        assertTrue(dto.active());
    }

    @Test
    void shouldCreateFromProductDefaults() {
        Product product = new Product(
                DEFAULT_NAME,
                List.of("cat-2"),
                DEFAULT_STOCK,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                null,
                null,
                null,
                false,
                DEFAULT_SELLER_ID);
        product.setId(DEFAULT_ID);

        ProductDetailDTO dto = ProductDetailDTO.from(product);

        assertEquals(DEFAULT_ID, dto.id());
        assertEquals("cat-2", dto.category());
        assertNull(dto.sellerName());
        assertFalse(dto.active());
    }
}

