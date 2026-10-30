package es.uclm.esibuy.orders.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mongodb.client.result.UpdateResult;

import es.uclm.esibuy.cart.dto.ItemCarritoDTO;
import es.uclm.esibuy.cart.model.Carrito;
import es.uclm.esibuy.cart.model.ItemCarrito;
import es.uclm.esibuy.cart.repository.CarritoRepository;
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

    private final UserRepository userRepository;
    private final CarritoRepository carritoRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;
    private final PaymentGateway paymentGateway;

    public OrderService(
            UserRepository userRepository,
            CarritoRepository carritoRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            MongoTemplate mongoTemplate,
            PaymentGateway paymentGateway) {
        this.userRepository = userRepository;
        this.carritoRepository = carritoRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.mongoTemplate = mongoTemplate;
        this.paymentGateway = paymentGateway;
    }

    public CheckoutSummaryDTO getCheckoutSummary(
            String customerId,
            String sellerId,
            List<String> productIds) {

        User customer = getCustomer(customerId);
        ClientProfile profile = customer.getClientProfile();

        Carrito cart = carritoRepository.findByClienteId(customerId)
                .orElseGet(() -> new Carrito(customerId));

        List<ShippingAddressResponse> addresses = profile.getAddresses().stream()
                .map(ShippingAddressResponse::from)
                .toList();

        List<CheckoutPaymentMethodDTO> paymentMethods = profile.getPaymentMethods().stream()
                .map(CheckoutPaymentMethodDTO::from)
                .toList();

        List<ItemCarritoDTO> items = cart.getItems().stream()
                .map(this::toCartItemDTO)
                .filter(item -> sellerId == null
                        || sellerId.isBlank()
                        || sellerId.equals(item.vendedorId()))
                .filter(item -> productIds == null
                        || productIds.isEmpty()
                        || productIds.contains(item.productoId()))
                .toList();

        BigDecimal totalPrice = items.stream()
                .map(ItemCarritoDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutSummaryDTO(addresses, paymentMethods, items, totalPrice);
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

        Carrito cart = carritoRepository.findByClienteId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("El carrito está vacío"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }

        List<OrderItem> items = prepareOrderItems(
                cart,
                request.sellerId(),
                request.productIds());

        BigDecimal totalPrice = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        carritoRepository.save(cart);

        return new OrderConfirmationDTO(
                savedOrder.getId(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus(),
                savedOrder.getCreatedAt(),
                savedOrder.getPaymentStatus());
    }

    public List<SellerOrderDTO> getSellerOrders(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("El identificador del vendedor es obligatorio");
        }

        Set<String> sellerProductIds = productRepository.findAllBySellerId(sellerId).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        return orderRepository.findAll().stream()
                .map(order -> toSellerOrder(order, sellerProductIds))
                .filter(Objects::nonNull)
                .toList();
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
            throw new IllegalArgumentException("El identificador del pedido es obligatorio");
        }

        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(PedidoNotFoundException::new);

        return toCustomerOrder(order);
    }

    public ShippingTrackingDTO getShippingStatusHistory(String customerId, String orderId) {
        getCustomer(customerId);

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("El identificador del pedido es obligatorio");
        }

        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(PedidoNotFoundException::new);

        List<OrderStatusHistoryDTO> statusHistory = order.getStatusHistory().stream()
                .map(event -> new OrderStatusHistoryDTO(event.status(), event.date()))
                .toList();

        return new ShippingTrackingDTO(statusHistory, order.getEstimatedDeliveryDate());
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

        if (isStatusRegression(order.getStatus(), status)
                && (justification == null || justification.isBlank())) {
            throw new IllegalArgumentException(
                    "La justificación es obligatoria para retroceder el estado");
        }

        Set<String> sellerProductIds = productRepository.findAllBySellerId(sellerId).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        boolean belongsToSeller = order.getItems().stream()
                .anyMatch(item -> sellerProductIds.contains(item.productId()));

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

    private SellerOrderDTO toSellerOrder(Order order, Set<String> sellerProductIds) {
        List<SellerOrderItemDTO> items = order.getItems().stream()
                .filter(item -> sellerProductIds.contains(item.productId()))
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

        User customer = userRepository.findById(order.getCustomerId()).orElse(null);

        String customerName = customer == null
                ? "Cliente no disponible"
                : (customer.getFirstName() + " " + customer.getLastName()).trim();

        String customerEmail = customer == null ? null : customer.getEmail();

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
        return String.join(", ", List.of(
                address.street(),
                address.postalCode() + " " + address.city(),
                address.province()));
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
            Carrito cart,
            String sellerId,
            List<String> productIds) {

        List<OrderItem> items = new ArrayList<>();
        Set<String> sellers = new HashSet<>();

        for (ItemCarrito item : cart.getItems()) {
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
            ItemCarrito item,
            String sellerId,
            List<String> productIds,
            Set<String> sellers) {

        if (productIds != null
                && !productIds.isEmpty()
                && !productIds.contains(item.getProductoId())) {
            return Optional.empty();
        }

        Product product = productRepository.findById(item.getProductoId())
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

        if (product.getStock() < item.getCantidad()) {
            throw createInsufficientStockException(product);
        }

        sellers.add(itemSellerId);

        BigDecimal unitPrice = product.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getCantidad()));

        return Optional.of(new OrderItem(
                product.getId(),
                product.getName(),
                item.getCantidad(),
                unitPrice,
                subtotal,
                product.getImageUrl()));
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

    private StockInsuficienteException createInsufficientStockException(Product product) {
        return new StockInsuficienteException(
                "Stock insuficiente para " + product.getName());
    }

    private ItemCarritoDTO toCartItemDTO(ItemCarrito item) {
        String imageUrl = item.getImageUrl();
        String sellerId = null;
        String sellerName = null;

        if (item.getProductoId() != null) {
            Product product = productRepository.findById(item.getProductoId()).orElse(null);

            if (product != null) {
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = product.getImageUrl();
                }

                sellerId = product.getSellerId();
                sellerName = resolveSellerName(sellerId);
            }
        }

        return new ItemCarritoDTO(
                item.getProductoId(),
                item.getNombre(),
                item.getPrecioUnitario(),
                item.getCantidad(),
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