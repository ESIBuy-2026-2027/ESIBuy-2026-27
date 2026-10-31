package es.uclm.esibuy.orders.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("pedidos")
public class Order {

    @Id
    private String id;

    @Field("clienteId")
    private String customerId;

    private List<OrderItem> items;

    @Field("precioTotal")
    private BigDecimal totalPrice;

    @Field("direccionEnvio")
    private OrderShippingAddress shippingAddress;

    @Field("metodoPago")
    private OrderPaymentMethod paymentMethod;

    @Field("estado")
    private OrderStatus status;

    @Field("estadoPago")
    private OrderPaymentStatus paymentStatus;

    @Field("fechaCreacion")
    private LocalDateTime createdAt;

    @Field("historialEstados")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    private LocalDate estimatedDeliveryDate;

    protected Order() {
    }

    public Order(
            String customerId,
            List<OrderItem> items,
            BigDecimal totalPrice,
            OrderShippingAddress shippingAddress,
            OrderPaymentMethod paymentMethod,
            OrderStatus status) {
        this(
                customerId,
                items,
                totalPrice,
                shippingAddress,
                paymentMethod,
                status,
                OrderPaymentStatus.PAGADO);
    }

    public Order(
            String customerId,
            List<OrderItem> items,
            BigDecimal totalPrice,
            OrderShippingAddress shippingAddress,
            OrderPaymentMethod paymentMethod,
            OrderStatus status,
            OrderPaymentStatus paymentStatus) {

        this.customerId = Objects.requireNonNull(customerId);
        this.items = List.copyOf(items);
        this.totalPrice = Objects.requireNonNull(totalPrice);
        this.shippingAddress = Objects.requireNonNull(shippingAddress);
        this.paymentMethod = Objects.requireNonNull(paymentMethod);
        this.status = Objects.requireNonNull(status);
        this.paymentStatus = Objects.requireNonNull(paymentStatus);
        this.createdAt = LocalDateTime.now();

        this.statusHistory.add(new OrderStatusHistory(status, createdAt));
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OrderShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public OrderPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderPaymentStatus getPaymentStatus() {
        return paymentStatus == null
                ? OrderPaymentStatus.PAGADO
                : paymentStatus;
    }

    public void setStatus(OrderStatus status) {
        OrderStatus newStatus = Objects.requireNonNull(status);

        if (this.status != newStatus) {
            LocalDateTime changeDate = LocalDateTime.now();
            this.status = newStatus;

            if (statusHistory == null) {
                statusHistory = new ArrayList<>();
            }

            statusHistory.add(new OrderStatusHistory(newStatus, changeDate));

            if (newStatus == OrderStatus.ENVIADO) {
                estimatedDeliveryDate = calculateEstimatedDeliveryDate(
                        changeDate.toLocalDate());
            } else if (newStatus != OrderStatus.ENTREGADO) {
                estimatedDeliveryDate = null;
            }
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return statusHistory == null
                ? List.of()
                : List.copyOf(statusHistory);
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    static LocalDate calculateEstimatedDeliveryDate(LocalDate shippingDate) {
        LocalDate estimatedDate = shippingDate;
        int businessDays = 0;

        while (businessDays < 3) {
            estimatedDate = estimatedDate.plusDays(1);

            DayOfWeek dayOfWeek = estimatedDate.getDayOfWeek();

            if (dayOfWeek != DayOfWeek.SATURDAY
                    && dayOfWeek != DayOfWeek.SUNDAY) {
                businessDays++;
            }
        }

        return estimatedDate;
    }

    public void setCustomerId(String customerId) {
        this.customerId = Objects.requireNonNull(customerId);
    }

    public void anonymizeCustomerData() {
        this.shippingAddress = new OrderShippingAddress(
                null,
                "Cliente eliminado",
                "Dirección no disponible",
                null,
                null,
                null,
                null,
                null);

        this.paymentMethod = null;
    }

    public void assignSellerToItems(String sellerId, List<String> productIds) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("El vendedor es obligatorio");
        }

        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        this.items = this.items.stream()
                .map(item -> productIds.contains(item.productId())
                        ? item.withSellerId(sellerId)
                        : item)
                .toList();
    }
}