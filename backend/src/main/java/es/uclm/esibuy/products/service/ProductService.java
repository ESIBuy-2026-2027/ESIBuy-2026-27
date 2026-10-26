package es.uclm.esibuy.products.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Arrays;
import es.uclm.esibuy.products.model.ProductDetails;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import es.uclm.esibuy.common.exception.CategoryNotFoundException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.products.dto.ProductCreateRequest;
import es.uclm.esibuy.products.dto.ProductDetailDTO;
import es.uclm.esibuy.products.dto.ProductResponse;
import es.uclm.esibuy.products.dto.ProductSummaryDTO;
import es.uclm.esibuy.products.dto.ProductSummaryResponse;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 5;
private static final String NO_PERMISSIONS_MESSAGE = "No tiene permisos para modificar este producto";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Producto no encontrado";
    private static final String PRODUCTO_NO_ENCONTRADO_KEY = PRODUCT_NOT_FOUND_MESSAGE;
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 8000;
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;

    private static final String IMAGE_API_KEY = "/api/products/";
    private static final String IMAGE_ENDPOINT = "/image";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final GridFsTemplate gridFsTemplate;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            GridFsTemplate gridFsTemplate) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.gridFsTemplate = gridFsTemplate;
    }

    public Product createProduct(
            ProductCreateRequest request,
            String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);
        List<String> categoryIds = validateCategories(request);

        Product product = new Product(new ProductDetails(
                request.getName().trim(),
                categoryIds,
                request.getStock() == null ? 0 : request.getStock(),
                request.getDescription(),
                request.getPrice(),
                request.getDiscount(),
                request.getPremiumDiscount(),
                null,
                Boolean.TRUE.equals(request.getAvailable()),
                seller.getId()));

        return productRepository.save(product);
    }

    public Product createProductWithImage(
            ProductCreateRequest request,
            MultipartFile file,
            String authenticatedEmail) {

        // Validar la imagen antes de crear el producto.
        ImagePayload image = file == null || file.isEmpty()
                ? null
                : validateAndNormalizeImage(file);

        User seller = findSeller(authenticatedEmail);
        List<String> categoryIds = validateCategories(request);

        Product product = new Product(new ProductDetails(
                request.getName().trim(),
                categoryIds,
                request.getStock() == null ? 0 : request.getStock(),
                request.getDescription(),
                request.getPrice(),
                request.getDiscount(),
                request.getPremiumDiscount(),
                null,
                Boolean.TRUE.equals(request.getAvailable()),
                seller.getId()));

        // Preparar el ID antes del guardado para no guardar dos veces.
        String productId = new ObjectId().toHexString();
        product.setId(productId);

        String fileId = null;

        try {
            if (image != null) {
                fileId = storeImage(image, file.getOriginalFilename());

                product.setImageFileId(fileId);
                product.setImageContentType(image.contentType());
                product.setImageUrl(IMAGE_API_KEY + productId + IMAGE_ENDPOINT);
            }

            // Una única escritura del producto.
            return productRepository.save(product);

        } catch (RuntimeException exception) {
            if (product.getId() != null) {
                try {
                    productRepository.deleteById(product.getId());
                } catch (RuntimeException cleanup) {
                    exception.addSuppressed(cleanup);
                }
            }

            if (fileId != null) {
                try {
                    deleteStoredImage(fileId);
                } catch (RuntimeException cleanup) {
                    exception.addSuppressed(cleanup);
                }
            }

            throw exception;
        }
    }

    /**
     * Actualiza producto e imagen; si la imagen no se envía, conserva la actual.
     */
    public Product updateProductWithImage(String id, ProductCreateRequest request, MultipartFile file,
            String authenticatedEmail) {
        ImagePayload image = file == null || file.isEmpty() ? null : validateAndNormalizeImage(file);
        Product product = getProduct(id, authenticatedEmail);
        List<String> categoryIds = validateCategories(request);
        String oldFileId = product.getImageFileId();
        String newFileId = null;
        if (image != null) {
            newFileId = storeImage(image, file.getOriginalFilename());
            product.setImageFileId(newFileId);
            product.setImageContentType(image.contentType());
            product.setImageUrl(IMAGE_API_KEY + id + IMAGE_ENDPOINT);
        }
        product.setName(request.getName().trim());
        product.setCategoryIds(categoryIds);
        product.setStock(request.getStock() == null ? 0 : request.getStock());
        product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        product.setPrice(request.getPrice());
        product.setDiscount(request.getDiscount());
        product.setPremiumDiscount(request.getPremiumDiscount());
        product.setAvailable(Boolean.TRUE.equals(request.getAvailable()));
        Product saved;
        try {
            saved = productRepository.save(product);
        } catch (RuntimeException exception) {
            if (newFileId != null) {
                try {
                    deleteStoredImage(newFileId);
                } catch (RuntimeException cleanup) {
                    exception.addSuppressed(cleanup);
                }
            }
            throw exception;
        }
        if (newFileId != null && oldFileId != null) {
            try {
                deleteStoredImage(oldFileId);
            } catch (RuntimeException cleanup) {
                /* La imagen antigua puede limpiarse posteriormente. */ }
        }
        return saved;
    }

    /** Guarda una imagen ya validada en GridFS y devuelve su identificador. */
    private String storeImage(ImagePayload image, String filename) {
        ObjectId id = gridFsTemplate.store(new ByteArrayInputStream(image.bytes()),
                filename == null || filename.isBlank() ? "product-image" : filename,
                image.contentType());
        return id.toHexString();
    }

    private void deleteStoredImage(String fileId) {
        if (fileId == null || fileId.isBlank())
            return;
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(new ObjectId(fileId))));
    }

    /** Reemplaza la imagen de un producto existente. */
    public Product uploadProductImage(String id, MultipartFile file, String authenticatedEmail) {
        Product product = getProduct(id, authenticatedEmail);
        ImagePayload image = validateAndNormalizeImage(file);
        String oldFileId = product.getImageFileId();
        String newFileId = storeImage(image, file.getOriginalFilename());
        product.setImageFileId(newFileId);
        product.setImageContentType(image.contentType());
        product.setImageUrl(IMAGE_API_KEY + id + IMAGE_ENDPOINT);
        Product saved;
        try {
            saved = productRepository.save(product);
        } catch (RuntimeException exception) {
            try {
                deleteStoredImage(newFileId);
            } catch (RuntimeException cleanup) {
                exception.addSuppressed(cleanup);
            }
            throw exception;
        }
        if (oldFileId != null) {
            try {
                deleteStoredImage(oldFileId);
            } catch (RuntimeException cleanup) {
                /* La imagen antigua puede limpiarse posteriormente. */ }
        }
        return saved;
    }

    public ProductImage getProductImage(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCTO_NO_ENCONTRADO_KEY));
        if (product.getImageFileId() == null || product.getImageContentType() == null) {
            throw new ProductNotFoundException("El producto no tiene imagen");
        }
        GridFSFile file = gridFsTemplate
                .findOne(Query.query(Criteria.where("_id").is(new ObjectId(product.getImageFileId()))));
        if (file == null)
            throw new ProductNotFoundException("La imagen del producto no existe");
        try {
            GridFsResource resource = gridFsTemplate.getResource(file);
            return new ProductImage(resource.getInputStream().readAllBytes(), product.getImageContentType());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer la imagen.");
        }
    }

    public record ProductImage(byte[] bytes, String contentType) {

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ProductImage(byte[] otherBytes, String otherContentType))) {
                return false;
            }
            return Arrays.equals(bytes, otherBytes)
                    && java.util.Objects.equals(contentType, otherContentType);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(bytes)
                    + java.util.Objects.hashCode(contentType);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    + "[bytes.length=" + (bytes == null ? 0 : bytes.length)
                    + ", contentType=" + contentType + "]";
        }
    }

    private record ImagePayload(byte[] bytes, String contentType) {

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ImagePayload(byte[] otherBytes, String otherContentType))) {
                return false;
            }
            return Arrays.equals(bytes, otherBytes)
                    && java.util.Objects.equals(contentType, otherContentType);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(bytes)
                    + java.util.Objects.hashCode(contentType);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                    + "[bytes.length=" + (bytes == null ? 0 : bytes.length)
                    + ", contentType=" + contentType + "]";
        }
    }

    private ImagePayload validateAndNormalizeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badImageRequest("Selecciona una imagen.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatusCode.valueOf(413),
                    "La imagen no puede superar los 5 MB.");
        }

        String declaredMime = getDeclaredMime(file);
        String extension = getExtension(file.getOriginalFilename());

        try {
            byte[] original = file.getBytes();
            String detectedMime = detectImageMime(original);

            validateImageFormat(detectedMime, declaredMime, extension);
            validateImageContent(original);

            return new ImagePayload(original, detectedMime);
        } catch (IOException ex) {
            throw badImageRequest("La imagen no es válida o está dañada.");
        }
    }

    private String getDeclaredMime(MultipartFile file) {
        String mime = file.getContentType();

        if (mime == null || mime.isBlank()) {
            throw badImageRequest("El tipo MIME de la imagen es obligatorio.");
        }

        return mime.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String getExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') < 0) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(java.util.Locale.ROOT);
    }

    private void validateImageFormat(
            String detectedMime,
            String declaredMime,
            String extension) {

        if (detectedMime == null || !detectedMime.equals(declaredMime)) {
            throw badImageRequest(
                    "El contenido de la imagen no coincide con su tipo MIME.");
        }

        boolean validExtension = "image/png".equals(detectedMime)
                ? "png".equals(extension)
                : "jpg".equals(extension) || "jpeg".equals(extension);

        if (!validExtension) {
            throw badImageRequest(
                    "La extensión del archivo no coincide con el formato de imagen permitido.");
        }
    }

    private void validateImageContent(byte[] original) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(original))) {

            if (input == null) {
                throw badImageRequest("La imagen no es válida.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw badImageRequest("La imagen no es válida o está dañada.");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0
                        || width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw badImageRequest(
                            "Las dimensiones de la imagen superan el límite permitido.");
                }

                if (reader.read(0) == null) {
                    throw badImageRequest(
                            "La imagen no se puede decodificar.");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private ResponseStatusException badImageRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String detectImageMime(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
                && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        return null;
    }

    public ProductSummaryResponse getSummary(String authenticatedEmail) {
        String sellerId = findSeller(authenticatedEmail).getId();

        return new ProductSummaryResponse(
                productRepository.countBySellerId(sellerId),
                productRepository.countBySellerIdAndStock(sellerId, 0),
                productRepository.countBySellerIdAndStockGreaterThanAndStockLessThanEqual(
                        sellerId, 0, LOW_STOCK_THRESHOLD),
                LOW_STOCK_THRESHOLD);
    }

    public List<ProductResponse> getSellerProducts(String authenticatedEmail) {
        String sellerId = findSeller(authenticatedEmail).getId();

        return productRepository.findAllBySellerId(sellerId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public Product getProduct(String id, String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(ProductNotFoundException::new);

        if (!seller.getId().equals(product.getSellerId())) {
            throw new ProductNotFoundException();
        }

        return product;
    }

    public Product updateProduct(
            String id,
            ProductCreateRequest request,
            String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(ProductNotFoundException::new);

        if (!seller.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException(NO_PERMISSIONS_MESSAGE);
        }

        List<String> categoryIds = validateCategories(request);

        product.setName(request.getName().trim());
        product.setCategoryIds(categoryIds);
        product.setStock(request.getStock() == null ? 0 : request.getStock());
        product.setDescription(
                request.getDescription() != null
                        ? request.getDescription().trim()
                        : null);
        product.setPrice(request.getPrice());
        product.setDiscount(request.getDiscount());
        product.setPremiumDiscount(request.getPremiumDiscount());
        product.setAvailable(Boolean.TRUE.equals(request.getAvailable()));

        return productRepository.save(product);
    }

    public List<ProductResponse> getAvailableProducts() {
        return productRepository.findAllByAvailableTrueAndStockGreaterThan(0).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> getPublicCatalog(
            String query,
            String categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating,
            Boolean inStock) {

        return productRepository.findAllByAvailableTrue().stream()
                .filter(product -> matchesQuery(product, query))
                .filter(product -> matchesCategory(product, categoryId))
                .filter(product -> matchesMinPrice(product, minPrice))
                .filter(product -> matchesMaxPrice(product, maxPrice))
                .filter(product -> matchesMinRating(product, minRating))
                .filter(product -> matchesStock(product, inStock))
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductSummaryDTO> getProductsForAdmin(
            String sellerId,
            String categoryId,
            Boolean inStock) {

        List<Product> products;
        if (sellerId != null && !sellerId.isBlank()) {
            products = productRepository.findAllBySellerId(sellerId.trim());
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .filter(product -> matchesCategory(product, categoryId))
                .filter(product -> matchesStockFilter(product, inStock))
                .map(this::toProductSummaryDTO)
                .toList();
    }

    public ProductDetailDTO getProductDetailForAdmin(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));
        String categoryName = resolveCategoryName(product);
        String sellerName = resolveSellerName(product.getSellerId());
        return ProductDetailDTO.from(product, categoryName, sellerName);
    }

    public void deleteProduct(String id, String authenticatedEmail) {
        User seller = findSeller(authenticatedEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        if (!seller.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException(NO_PERMISSIONS_MESSAGE);
        }

        productRepository.delete(product);
    }

    private ProductSummaryDTO toProductSummaryDTO(Product product) {
        String categoryName = resolveCategoryName(product);
        String sellerName = resolveSellerName(product.getSellerId());
        return ProductSummaryDTO.from(product, categoryName, sellerName);
    }

    private String resolveCategoryName(Product product) {
        if (product.getCategoryIds() == null || product.getCategoryIds().isEmpty()) {
            return null;
        }
        String firstCategoryId = product.getCategoryIds().get(0);
        return categoryRepository.findById(firstCategoryId)
                .map(Category::getName)
                .orElse(firstCategoryId);
    }

    private String resolveSellerName(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            return null;
        }
        return userRepository.findById(sellerId)
                .map(user -> {
                    if (user.getSellerProfile() != null
                            && user.getSellerProfile().getBusinessName() != null
                            && !user.getSellerProfile().getBusinessName().isBlank()) {
                        return user.getSellerProfile().getBusinessName();
                    }
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    return fullName.isBlank() ? user.getEmail() : fullName;
                })
                .orElse(null);
    }

    private boolean matchesStockFilter(Product product, Boolean inStock) {
        if (inStock == null) {
            return true;
        }
        if (Boolean.TRUE.equals(inStock)) {
            return product.getStock() > 0;
        }
        return product.getStock() <= 0;
    }

    private boolean matchesQuery(Product product, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase();

        boolean matchesName = product.getName() != null
                && product.getName().toLowerCase().contains(normalizedQuery);

        boolean matchesDescription = product.getDescription() != null
                && product.getDescription().toLowerCase().contains(normalizedQuery);

        return matchesName || matchesDescription;
    }

    private boolean matchesCategory(Product product, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return true;
        }

        return product.getCategoryIds() != null
                && product.getCategoryIds().contains(categoryId.trim());
    }

    private boolean matchesMinPrice(Product product, BigDecimal minPrice) {
        if (minPrice == null) {
            return true;
        }

        return product.getPrice() != null
                && product.getPrice().compareTo(minPrice) >= 0;
    }

    private boolean matchesMaxPrice(Product product, BigDecimal maxPrice) {
        if (maxPrice == null) {
            return true;
        }

        return product.getPrice() != null
                && product.getPrice().compareTo(maxPrice) <= 0;
    }

    private boolean matchesMinRating(Product product, Double minRating) {
        if (minRating == null) {
            return true;
        }

        return product.getRating() != null
                && product.getRating() >= minRating;
    }

    private boolean matchesStock(Product product, Boolean inStock) {
        if (!Boolean.TRUE.equals(inStock)) {
            return true;
        }

        return product.getStock() > 0;
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(
                        () -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        return ProductResponse.from(product);
    }

    public ProductResponse updateProductStatus(
            String id,
            boolean available,
            String authenticatedEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(
                        () -> new ProductNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException("Autenticación requerida");
        }

        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(
                        () -> new AccessDeniedException("Usuario no encontrado"));

        if (user.getRole() != UserRole.ADMIN
                && !user.getId().equals(product.getSellerId())) {
            throw new AccessDeniedException(NO_PERMISSIONS_MESSAGE);
        }

        product.setAvailable(available);

        return ProductResponse.from(productRepository.save(product));
    }

    private User findSeller(String authenticatedEmail) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new AccessDeniedException(
                    "Se requiere una sesión de vendedor autenticada");
        }

        User seller = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(
                        () -> new AccessDeniedException(
                                "No se encontró el vendedor autenticado"));

        if (seller.getRole() != UserRole.SELLER) {
            throw new AccessDeniedException(
                    "Solo un vendedor puede acceder a este recurso");
        }

        return seller;
    }

    private List<String> validateCategories(ProductCreateRequest request) {
        List<String> categoryIds = List.copyOf(request.getCategoryIds());

        Set<String> existingCategoryIds = categoryRepository.findAllById(categoryIds).stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<String> missingCategoryIds = categoryIds.stream()
                .filter(categoryId -> !existingCategoryIds.contains(categoryId))
                .distinct()
                .toList();

        if (!missingCategoryIds.isEmpty()) {
            throw new CategoryNotFoundException(missingCategoryIds);
        }

        return categoryIds;
    }
}
