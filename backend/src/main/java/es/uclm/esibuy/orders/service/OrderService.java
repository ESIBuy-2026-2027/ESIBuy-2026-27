package es.uclm.esibuy.orders.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mongodb.client.result.UpdateResult;

import es.uclm.esibuy.cart.dto.CartItemDTO;
import es.uclm.esibuy.cart.model.Cart;
import es.uclm.esibuy.cart.model.CartItem;
import es.uclm.esibuy.cart.repository.CartRepository;
import es.uclm.esibuy.configuration.model.GeneralConfiguration;
import es.uclm.esibuy.configuration.service.GeneralConfigurationService;
import es.uclm.esibuy.common.exception.PagoFallidoException;
import es.uclm.esibuy.common.exception.PedidoNotFoundException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.RecursoCheckoutNoEncontradoException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.orders.dto.CheckoutPaymentMethodDTO;
import es.uclm.esibuy.orders.dto.CheckoutSummaryDTO;
import es.uclm.esibuy.orders.dto.CreateOrderDTO;
import es.uclm.esibuy.orders.dto.CustomerOrderDTO;
import es.uclm.esibuy.orders.dto.CustomerOrderItemDTO;
import es.uclm.esibuy.orders.dto.CustomerPaymentMethodDTO;
import es.uclm.esibuy.orders.dto.CustomerShippingAddressDTO;
import es.uclm.esibuy.orders.dto.OrderConfirmationDTO;
import es.uclm.esibuy.orders.dto.OrderStatusHistoryDTO;
import es.uclm.esibuy.orders.dto.SellerOrderDTO;
import es.uclm.esibuy.orders.dto.SellerOrderItemDTO;
import es.uclm.esibuy.orders.dto.ShippingTrackingDTO;
import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderItem;
import es.uclm.esibuy.orders.model.OrderPaymentMethod;
import es.uclm.esibuy.orders.model.OrderPaymentStatus;
import es.uclm.esibuy.orders.model.OrderShippingAddress;
import es.uclm.esibuy.orders.model.OrderStatus;
import es.uclm.esibuy.orders.repository.OrderRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.dto.ShippingAddressResponse;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.ShippingAddress;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.repository.UserRepository;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final String STOCK_FIELD = "stock";
    private static final String ORDER_ID_REQUIRED_MESSAGE =
            "El identificador del pedido es obligatorio";

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;
    private final PaymentGateway paymentGateway;
    private final GeneralConfigurationService generalConfigurationService;

    public OrderService(
            UserRepository userRepository,
            CartRepository cartRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            MongoTemplate mongoTemplate,
            PaymentGateway paymentGateway,
            GeneralConfigurationService generalConfigurationService) {

        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.mongoTemplate = mongoTemplate;
        this.paymentGateway = paymentGateway;
        this.generalConfigurationService = generalConfigurationService;
    }

    public CheckoutSummaryDTO getCheckoutSummary(
            String customerId,
            String sellerId,
            List<String> productIds) {

        User customer = getCustomer(customerId);
        ClientProfile profile = customer.getClientProfile();

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> new Cart(customerId));

        List<ShippingAddressResponse> addresses = profile.getAddresses().stream()
                .map(ShippingAddressResponse::from)
                .toList();

        List<CheckoutPaymentMethodDTO> paymentMethods = profile.getPaymentMethods().stream()
                .map(CheckoutPaymentMethodDTO::from)
                .toList();

        List<CartItemDTO> items = cart.getItems().stream()
                .map(this::toCartItemDTO)
                .filter(item -> sellerId == null
                        || sellerId.isBlank()
                        || sellerId.equals(item.sellerId()))
                .filter(item -> productIds == null
                        || productIds.isEmpty()
                        || productIds.contains(item.productId()))
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        GeneralConfiguration configuration = generalConfigurationService.getCurrent();
        OrderPricing pricing = calculatePricing(subtotal, configuration);

        return new CheckoutSummaryDTO(
                addresses,
                paymentMethods,
                items,
                pricing.subtotal(),
                pricing.taxPercentage(),
                pricing.taxAmount(),
                pricing.shippingCost(),
                pricing.total());
    }

    public CheckoutSummaryDTO getCheckoutSummary(String customerId, String sellerId) {
        return getCheckoutSummary(customerId, sellerId, null);
    }

    public CheckoutSummaryDTO getCheckoutSummary(String customerId) {
        return getCheckoutSummary(customerId, null);
    }

    public OrderConfirmationDTO createOrder(String customerId, CreateOrderDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud del pedido es obligatoria");
        }

        User customer = getCustomer(customerId);

        ShippingAddress shippingAddress = getShippingAddress(
                customer.getClientProfile(),
                request.shippingAddressId());

        PaymentMethod paymentMethod = getPaymentMethod(
                customer.getClientProfile(),
                request.paymentMethodId());

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("El carrito está vacío"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }

        List<OrderItem> items = prepareOrderItems(
                cart,
                request.sellerId(),
                request.productIds());

        BigDecimal subtotal = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        GeneralConfiguration configuration = generalConfigurationService.getCurrent();
        BigDecimal totalPrice = calculatePricing(subtotal, configuration).total();

        reserveStock(items);

        OrderPaymentMethod orderPaymentMethod = OrderPaymentMethod.from(paymentMethod);
        boolean paymentAccepted;

        try {
            paymentAccepted = paymentGateway.charge(orderPaymentMethod, totalPrice);
        } catch (RuntimeException exception) {
            releaseStock(items, exception);
            throw exception;
        }

        if (!paymentAccepted) {
            PagoFallidoException paymentException = new PagoFallidoException();
            releaseStock(items, paymentException);
            throw paymentException;
        }

        Order order = new Order(
                customerId,
                items,
                totalPrice,
                OrderShippingAddress.from(shippingAddress),
                orderPaymentMethod,
                OrderStatus.PENDIENTE,
                OrderPaymentStatus.PAGADO);

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : items) {
            cart.removeItem(item.productId());
        }

        cartRepository.save(cart);

        return new OrderConfirmationDTO(
                savedOrder.getId(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus(),
                savedOrder.getCreatedAt(),
                savedOrder.getPaymentStatus());
    }

    private OrderPricing calculatePricing(
            BigDecimal subtotal,
            GeneralConfiguration configuration) {
        BigDecimal ivaPorcentaje = configuration.getIvaPorcentaje();
        BigDecimal divisor = BigDecimal.valueOf(100).add(ivaPorcentaje);
        BigDecimal taxAmount = divisor.compareTo(BigDecimal.ZERO) > 0
                ? subtotal.multiply(ivaPorcentaje).divide(divisor, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);
        BigDecimal shippingCost = subtotal.compareTo(configuration.getEnvioGratuitoDesde()) >= 0
                ? BigDecimal.ZERO.setScale(2)
                : configuration.getGastosEnvio().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shippingCost)
                .setScale(2, RoundingMode.HALF_UP);

        return new OrderPricing(
                subtotal,
                configuration.getIvaPorcentaje(),
                taxAmount,
                shippingCost,
                total);
    }

    private record OrderPricing(
            BigDecimal subtotal,
            BigDecimal taxPercentage,
            BigDecimal taxAmount,
            BigDecimal shippingCost,
            BigDecimal total) {
    }

    public List<SellerOrderDTO> getSellerOrders(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("El identificador del vendedor es obligatorio");
        }

        Set<String> sellerProductIds = productRepository.findAllBySellerId(sellerId).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        List<Order> sellerOrders = orderRepository
                .findAllByItemsSellerIdOrItemsProductIdInOrderByCreatedAtDesc(sellerId, sellerProductIds);
        Map<String, User> customersById = loadCustomers(sellerOrders);

        return sellerOrders.stream()
                .sorted(Comparator.comparing(
                        Order::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(order -> toSellerOrder(order, sellerId, sellerProductIds, customersById))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, User> loadCustomers(List<Order> orders) {
        Set<String> customerIds = orders.stream()
                .map(Order::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, User> customersById = new HashMap<>();
        userRepository.findAllById(customerIds)
                .forEach(customer -> customersById.put(customer.getId(), customer));
        return customersById;
    }

    public List<CustomerOrderDTO> getCustomerOrders(String customerId) {
        getCustomer(customerId);

        return orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toCustomerOrder)
                .toList();
    }

    public CustomerOrderDTO getCustomerOrder(String customerId, String orderId) {
        getCustomer(customerId);

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(ORDER_ID_REQUIRED_MESSAGE);
        }

        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(PedidoNotFoundException::new);

        return toCustomerOrder(order);
    }

    public void cancelCustomerOrder(String customerId, String orderId) {
        getCustomer(customerId);

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(ORDER_ID_REQUIRED_MESSAGE);
        }

        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(PedidoNotFoundException::new);

        if (order.getStatus() != OrderStatus.PENDIENTE
                || order.getPaymentStatus() != OrderPaymentStatus.PAGADO) {
            throw new IllegalArgumentException(
                    "El pedido ya no se puede cancelar en su estado actual");
        }

        restoreCancelledOrderStock(order.getItems());
        order.setStatus(OrderStatus.CANCELADO);
        orderRepository.save(order);
    }

    public ShippingTrackingDTO getShippingStatusHistory(String customerId, String orderId) {
        getCustomer(customerId);

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(ORDER_ID_REQUIRED_MESSAGE);
        }

        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(PedidoNotFoundException::new);

        List<OrderStatusHistoryDTO> statusHistory = order.getStatusHistory().stream()
                .map(event -> new OrderStatusHistoryDTO(event.status(), event.date()))
                .toList();

        return new ShippingTrackingDTO(statusHistory, order.getEstimatedDeliveryDate());
    }

    public void confirmOrderReceipt(String customerId, String orderId) {
        getCustomer(customerId);

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(ORDER_ID_REQUIRED_MESSAGE);
        }

        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(PedidoNotFoundException::new);

        if (order.getStatus() != OrderStatus.ENVIADO) {
            throw new IllegalArgumentException(
                    "Solo se puede confirmar la recepción de un pedido enviado");
        }

        order.setStatus(OrderStatus.ENTREGADO);
        orderRepository.save(order);
    }

    public void updateOrderStatus(
            String sellerId,
            String orderId,
            OrderStatus status,
            String justification) {

        if (sellerId == null || sellerId.isBlank()
                || orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("El vendedor y el pedido son obligatorios");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe"));

        if (status == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }

        if (status == OrderStatus.ENTREGADO) {
            throw new IllegalArgumentException(
                    "La recepción del pedido debe confirmarla el cliente");
        }

        validateStatusTransition(order.getStatus(), status);

        if (isStatusRegression(order.getStatus(), status)
                && (justification == null || justification.isBlank())) {
            throw new IllegalArgumentException(
                    "La justificación es obligatoria para retroceder el estado");
        }

        Set<String> sellerProductIds = productRepository.findAllBySellerId(sellerId).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        boolean belongsToSeller = order.getItems().stream()
                .anyMatch(item -> sellerId.equals(item.sellerId())
                        || sellerProductIds.contains(item.productId()));

        if (!belongsToSeller) {
            throw new IllegalArgumentException("El pedido no pertenece al vendedor");
        }

        order.setStatus(status);
        orderRepository.save(order);
    }

    private boolean isStatusRegression(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null) {
            return false;
        }

        List<OrderStatus> workflow = List.of(
                OrderStatus.PENDIENTE,
                OrderStatus.PREPARANDO,
                OrderStatus.ENVIADO,
                OrderStatus.ENTREGADO);

        int currentPosition = workflow.indexOf(currentStatus);
        int newPosition = workflow.indexOf(newStatus);

        return currentPosition >= 0
                && newPosition >= 0
                && newPosition < currentPosition;
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null || currentStatus == newStatus) {
            return;
        }

        if (currentStatus == OrderStatus.CANCELADO || currentStatus == OrderStatus.ENTREGADO) {
            throw new IllegalArgumentException(
                    "No se puede modificar un pedido que ya está " + currentStatus.name().toLowerCase());
        }

        if (newStatus == OrderStatus.CANCELADO) {
            validateCancellationTransition(currentStatus);
            return;
        }

        // Si es un retroceso en el flujo normal, se permite siempre que se justifique (validado aparte)
        if (isStatusRegression(currentStatus, newStatus)) {
            return;
        }

        // Validación de avance secuencial: no permitir saltar fases
        validateForwardTransition(currentStatus, newStatus);
    }

    private void validateCancellationTransition(OrderStatus currentStatus) {
        if (currentStatus != OrderStatus.PENDIENTE
                && currentStatus != OrderStatus.PAGADO
                && currentStatus != OrderStatus.PREPARANDO) {
            throw new IllegalArgumentException(
                    "Un pedido enviado o entregado ya no puede ser cancelado");
        }
    }

    private void validateForwardTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.PENDIENTE || currentStatus == OrderStatus.PAGADO) {
            if (newStatus != OrderStatus.PREPARANDO) {
                throw new IllegalArgumentException(
                        "Un pedido pendiente debe pasar a preparación antes de enviarse");
            }
        } else if (currentStatus == OrderStatus.PREPARANDO) {
            if (newStatus != OrderStatus.ENVIADO) {
                throw new IllegalArgumentException(
                        "Un pedido en preparación solo puede pasar a enviado");
            }
        } else if (currentStatus == OrderStatus.ENVIADO) {
            throw new IllegalArgumentException(
                    "Un pedido enviado no puede modificarse directamente a este estado");
        }
    }

    private SellerOrderDTO toSellerOrder(
            Order order,
            String sellerId,
            Set<String> sellerProductIds,
            Map<String, User> customersById) {
        List<SellerOrderItemDTO> items = order.getItems().stream()
                .filter(item -> sellerId.equals(item.sellerId())
                        || sellerProductIds.contains(item.productId()))
                .map(item -> new SellerOrderItemDTO(
                        item.productId(),
                        item.name(),
                        item.quantity(),
                        item.unitPrice(),
                        item.subtotal(),
                        resolveImageUrl(item),
                        resolveCategoryIds(item)))
                .toList();

        if (items.isEmpty()) {
            return null;
        }

        User customer = customersById.get(order.getCustomerId());

        String customerName = customer == null
                ? "Cliente no disponible"
                : (customer.getFirstName() + " " + customer.getLastName()).trim();

        String customerEmail = customer == null
                || "cliente.eliminado@esibuy.local".equalsIgnoreCase(customer.getEmail())
                        ? null
                        : customer.getEmail();

        BigDecimal total = items.stream()
                .map(SellerOrderItemDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SellerOrderDTO(
                order.getId(),
                order.getCreatedAt(),
                customerName,
                customerEmail,
                formatAddress(order.getShippingAddress()),
                items,
                total,
                order.getStatus());
    }

    private CustomerOrderDTO toCustomerOrder(Order order) {
        List<CustomerOrderItemDTO> items = order.getItems().stream()
                .map(item -> new CustomerOrderItemDTO(
                        item.productId(),
                        item.name(),
                        item.quantity(),
                        item.unitPrice(),
                        item.subtotal(),
                        resolveImageUrl(item)))
                .toList();

        OrderPaymentMethod paymentMethod = order.getPaymentMethod();

        return new CustomerOrderDTO(
                order.getId(),
                order.getCreatedAt(),
                items,
                order.getTotalPrice(),
                order.getStatus(),
                toCustomerShippingAddress(order.getShippingAddress()),
                paymentMethod == null
                        ? null
                        : new CustomerPaymentMethodDTO(
                                paymentMethod.type(),
                                paymentMethod.lastFourDigits()),
                order.getPaymentStatus());
    }

    private CustomerShippingAddressDTO toCustomerShippingAddress(
            OrderShippingAddress address) {

        return new CustomerShippingAddressDTO(
                address.recipientName(),
                address.street(),
                address.additionalInfo(),
                address.postalCode(),
                address.city(),
                address.province(),
                address.phone());
    }

    private String resolveImageUrl(OrderItem item) {
        if (item.imageUrl() != null && !item.imageUrl().isBlank()) {
            return item.imageUrl();
        }

        return productRepository.findById(item.productId())
                .map(Product::getImageUrl)
                .orElse(null);
    }

    private List<String> resolveCategoryIds(OrderItem item) {
        return productRepository.findById(item.productId())
                .map(Product::getCategoryIds)
                .orElse(List.of());
    }

    private String formatAddress(OrderShippingAddress address) {
        if (address == null) {
            return "";
        }

        String postalCodeAndCity = Stream.of(
                address.postalCode(),
                address.city())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));

        return Stream.of(
                address.street(),
                postalCodeAndCity,
                address.province())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private User getCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(UserNotFoundException::new);

        if (customer.getClientProfile() == null) {
            throw new UserNotFoundException();
        }

        return customer;
    }

    private ShippingAddress getShippingAddress(ClientProfile profile, String shippingAddressId) {
        return profile.getAddresses().stream()
                .filter(address -> address.getId().equals(shippingAddressId))
                .findFirst()
                .orElseThrow(RecursoCheckoutNoEncontradoException::new);
    }

    private PaymentMethod getPaymentMethod(ClientProfile profile, String paymentMethodId) {
        return profile.getPaymentMethods().stream()
                .filter(method -> method.getId().equals(paymentMethodId))
                .findFirst()
                .orElseThrow(RecursoCheckoutNoEncontradoException::new);
    }

    private List<OrderItem> prepareOrderItems(
            Cart cart,
            String sellerId,
            List<String> productIds) {

        List<OrderItem> items = new ArrayList<>();
        Set<String> sellers = new HashSet<>();

        for (CartItem item : cart.getItems()) {
            Optional<OrderItem> orderItem = prepareOrderItem(
                    item,
                    sellerId,
                    productIds,
                    sellers);

            orderItem.ifPresent(items::add);
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay productos válidos para tramitar este pedido.");
        }

        if (sellers.size() > 1) {
            throw new IllegalArgumentException(
                    "No se puede realizar un pedido con productos de diferentes vendedores. "
                            + "Debes realizar un pedido independiente por cada vendedor.");
        }

        return List.copyOf(items);
    }

    private Optional<OrderItem> prepareOrderItem(
            CartItem item,
            String sellerId,
            List<String> productIds,
            Set<String> sellers) {

        if (productIds != null
                && !productIds.isEmpty()
                && !productIds.contains(item.getProductId())) {
            return Optional.empty();
        }

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(ProductNotFoundException::new);

        String itemSellerId = product.getSellerId() != null
                ? product.getSellerId()
                : "SIN_VENDEDOR";

        if (sellerId != null
                && !sellerId.isBlank()
                && !sellerId.equals(itemSellerId)) {
            return Optional.empty();
        }

        if (!product.isAvailable()) {
            throw new ProductoNoDisponibleException();
        }

        if (product.getStock() < item.getQuantity()) {
            throw createInsufficientStockException(product);
        }

        sellers.add(itemSellerId);

        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : product.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return Optional.of(new OrderItem(
                product.getId(),
                product.getName(),
                item.getQuantity(),
                unitPrice,
                subtotal,
                product.getImageUrl(),
                product.getSellerId()));
    }

    private void reserveStock(List<OrderItem> items) {
        List<OrderItem> reservedItems = new ArrayList<>();

        try {
            for (OrderItem item : items) {
                Query query = Query.query(Criteria.where("_id").is(item.productId())
                        .and(STOCK_FIELD).gte(item.quantity())
                        .and("available").is(true));

                Product updatedProduct = mongoTemplate.findAndModify(
                        query,
                        new Update().inc(STOCK_FIELD, -item.quantity()),
                        FindAndModifyOptions.options().returnNew(true),
                        Product.class);

                if (updatedProduct == null) {
                    throw new StockInsuficienteException(
                            "Stock insuficiente para " + item.name());
                }

                reservedItems.add(item);
            }
        } catch (RuntimeException exception) {
            releaseStock(reservedItems, exception);
            throw exception;
        }
    }

    private void releaseStock(List<OrderItem> items, RuntimeException cause) {
        IllegalStateException releaseError = null;

        for (OrderItem item : items) {
            try {
                Query query = Query.query(Criteria.where("_id").is(item.productId()));

                UpdateResult result = mongoTemplate.updateFirst(
                        query,
                        new Update().inc(STOCK_FIELD, item.quantity()),
                        Product.class);

                if (result == null
                        || !result.wasAcknowledged()
                        || result.getMatchedCount() == 0) {
                    throw new IllegalStateException(
                            "No se pudo liberar una reserva de stock");
                }
            } catch (RuntimeException exception) {
                if (releaseError == null) {
                    releaseError = new IllegalStateException(
                            "No se pudo liberar el stock reservado",
                            cause);
                }

                releaseError.addSuppressed(exception);
            }
        }

        if (releaseError != null) {
            throw releaseError;
        }
    }

    private void restoreCancelledOrderStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No se pudo encontrar el producto para reponer el stock"));
            product.setStock(product.getStock() + item.quantity());
            productRepository.save(product);
        }
    }

    private StockInsuficienteException createInsufficientStockException(Product product) {
        return new StockInsuficienteException(
                "Stock insuficiente para " + product.getName());
    }

    private CartItemDTO toCartItemDTO(CartItem item) {
        String imageUrl = item.getImageUrl();
        String sellerId = null;
        String sellerName = null;

        if (item.getProductId() != null) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);

            if (product != null) {
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = product.getImageUrl();
                }

                sellerId = product.getSellerId();
                sellerName = resolveSellerName(sellerId);
            }
        }

        return new CartItemDTO(
                item.getProductId(),
                item.getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                imageUrl,
                sellerId,
                sellerName);
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

                    String firstName = user.getFirstName() != null
                            ? user.getFirstName()
                            : "";

                    String lastName = user.getLastName() != null
                            ? user.getLastName()
                            : "";

                    String fullName = (firstName + " " + lastName).trim();

                    return !fullName.isBlank()
                            ? fullName
                            : "Vendedor " + sellerId;
                })
                .orElse("Vendedor " + sellerId);
    }
}