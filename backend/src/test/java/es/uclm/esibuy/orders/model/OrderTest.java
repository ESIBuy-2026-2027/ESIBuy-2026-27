
package es.uclm.esibuy.orders.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import es.uclm.esibuy.users.model.PaymentMethodType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTest {

    @Test
    void shouldCalculateEstimatedDeliveryAfterThreeBusinessDaysAndSkipWeekend() {
        LocalDate shipmentDate = LocalDate.of(2026, 10, 29);

        LocalDate estimatedDeliveryDate = Order.calculateEstimatedDeliveryDate(shipmentDate);

        assertEquals(LocalDate.of(2026, 11, 3), estimatedDeliveryDate);
    }

    @Test
    void shouldCountBusinessDaysFromWeekendShipmentOnNextMonday() {
        LocalDate shipmentDate = LocalDate.of(2026, 10, 31);

        LocalDate estimatedDeliveryDate = Order.calculateEstimatedDeliveryDate(shipmentDate);

        assertEquals(LocalDate.of(2026, 11, 4), estimatedDeliveryDate);
    }

    @Test
    void shouldKeepPaymentStatusIndependentFromFulfillmentStatus() {
        Order order = new Order(
                "cliente-1",
                List.of(),
                BigDecimal.ZERO,
                new OrderShippingAddress(
                        "direccion-1",
                        "Cliente",
                        "Calle 1",
                        null,
                        "13001",
                        "Ciudad Real",
                        "Ciudad Real",
                        null),
                new OrderPaymentMethod("pago-1", PaymentMethodType.CARD, "1234"),
                OrderStatus.PENDIENTE,
                OrderPaymentStatus.PAGADO);

        assertEquals(OrderStatus.PENDIENTE, order.getStatus());
        assertEquals(OrderPaymentStatus.PAGADO, order.getPaymentStatus());

        order.setStatus(OrderStatus.PREPARANDO);

        assertEquals(OrderPaymentStatus.PAGADO, order.getPaymentStatus());
    }

    @Test
    void shouldTreatLegacyOrdersWithoutPaymentStatusAsPaid() {
        Order order = new Order(
                "cliente-1",
                List.of(),
                BigDecimal.ZERO,
                new OrderShippingAddress(
                        "direccion-1",
                        "Cliente",
                        "Calle 1",
                        null,
                        "13001",
                        "Ciudad Real",
                        "Ciudad Real",
                        null),
                new OrderPaymentMethod("pago-1", PaymentMethodType.CARD, "1234"),
                OrderStatus.PENDIENTE);

        org.springframework.test.util.ReflectionTestUtils.setField(
                order, "paymentStatus", null);

        assertEquals(OrderPaymentStatus.PAGADO, order.getPaymentStatus());
    }
}
