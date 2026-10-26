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

class ProductSummaryDTOTest {

    private static final String DEFAULT_ID = "prod-001";
    private static final String DEFAULT_NAME = "Wireless Headphones";
    private static final String DEFAULT_DESCRIPTION = "Noise cancelling wireless headphones";
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("89.99");
    private static final Integer DEFAULT_STOCK = 25;
    private static final String DEFAULT_CATEGORY = "Electronics";
    private static final String DEFAULT_SELLER_ID = "seller-123";
    private static final String DEFAULT_SELLER_NAME = "Audio Store";
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

    private ProductSummaryDTO createValidDTO() {
        return new ProductSummaryDTO(
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
    void shouldValidateValidProductSummaryDTO() {
        ProductSummaryDTO dto = createValidDTO();
        Set<ConstraintViolation<ProductSummaryDTO>> violations = validator.validate(dto);

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
        ProductSummaryDTO dto = new ProductSummaryDTO(
                "   ",
                DEFAULT_NAME,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                DEFAULT_STOCK,
                DEFAULT_CATEGORY,
                DEFAULT_SELLER_ID,
                DEFAULT_SELLER_NAME,
                DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductSummaryDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_ID)));
    }

    @Test
    void shouldRejectBlankName() {
        ProductSummaryDTO dto = new ProductSummaryDTO(
                DEFAULT_ID,
                "",
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                DEFAULT_STOCK,
                DEFAULT_CATEGORY,
                DEFAULT_SELLER_ID,
                DEFAULT_SELLER_NAME,
                DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductSummaryDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_NAME)));
    }

    @Test
    void shouldRejectNameExceedingMaximumLength() {
        String longName = "A".repeat(151);
        ProductSummaryDTO dto = new ProductSummaryDTO(
                DEFAULT_ID,
                longName,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                DEFAULT_STOCK,
                DEFAULT_CATEGORY,
                DEFAULT_SELLER_ID,
                DEFAULT_SELLER_NAME,
                DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductSummaryDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_NAME)));
    }

    @Test
    void shouldRejectNullOrZeroOrNegativePrice() {
        ProductSummaryDTO nullPriceDTO = new ProductSummaryDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, null, DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(nullPriceDTO).isEmpty());

        ProductSummaryDTO zeroPriceDTO = new ProductSummaryDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, BigDecimal.ZERO, DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(zeroPriceDTO).isEmpty());

        ProductSummaryDTO negativePriceDTO = new ProductSummaryDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, new BigDecimal("-5.00"), DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(negativePriceDTO).isEmpty());
    }

    @Test
    void shouldRejectNullOrNegativeStock() {
        ProductSummaryDTO nullStockDTO = new ProductSummaryDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, null,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(nullStockDTO).isEmpty());

        ProductSummaryDTO negativeStockDTO = new ProductSummaryDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, -1,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);
        assertFalse(validator.validate(negativeStockDTO).isEmpty());
    }

    @Test
    void shouldRejectBlankSellerId() {
        ProductSummaryDTO dto = new ProductSummaryDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, DEFAULT_STOCK,
                DEFAULT_CATEGORY, "  ", DEFAULT_SELLER_NAME, DEFAULT_ACTIVE);

        Set<ConstraintViolation<ProductSummaryDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_SELLER_ID)));
    }

    @Test
    void shouldRejectNullActive() {
        ProductSummaryDTO dto = new ProductSummaryDTO(
                DEFAULT_ID, DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_PRICE, DEFAULT_STOCK,
                DEFAULT_CATEGORY, DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, null);

        Set<ConstraintViolation<ProductSummaryDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(PROPERTY_ACTIVE)));
    }

    @Test
    void shouldCreateFromProductWithCategoryAndSeller() {
        Product product = new Product(
                DEFAULT_NAME,
                List.of("cat-1"),
                DEFAULT_STOCK,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                null,
                null,
                "https://example.com/img.jpg",
                true,
                DEFAULT_SELLER_ID);
        product.setId(DEFAULT_ID);

        ProductSummaryDTO dto = ProductSummaryDTO.from(product, DEFAULT_CATEGORY, DEFAULT_SELLER_NAME);

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
                List.of("cat-1"),
                DEFAULT_STOCK,
                DEFAULT_DESCRIPTION,
                DEFAULT_PRICE,
                null,
                null,
                null,
                false,
                DEFAULT_SELLER_ID);
        product.setId(DEFAULT_ID);

        ProductSummaryDTO dto = ProductSummaryDTO.from(product);

        assertEquals(DEFAULT_ID, dto.id());
        assertEquals("cat-1", dto.category());
        assertNull(dto.sellerName());
        assertFalse(dto.active());
    }
}

