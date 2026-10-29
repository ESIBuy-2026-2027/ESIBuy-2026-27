package es.uclm.esibuy.pedidos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import es.uclm.esibuy.users.model.PaymentMethodType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoTest {

    @Test
    void shouldCalculateEstimatedDeliveryAfterThreeBusinessDaysAndSkipWeekend() {
        LocalDate shipmentDate = LocalDate.of(2026, 10, 29);

        LocalDate estimatedDeliveryDate = Pedido.calcularFechaEstimadaEntrega(shipmentDate);

        assertEquals(LocalDate.of(2026, 11, 3), estimatedDeliveryDate);
    }

    @Test
    void shouldCountBusinessDaysFromWeekendShipmentOnNextMonday() {
        LocalDate shipmentDate = LocalDate.of(2026, 10, 31);

        LocalDate estimatedDeliveryDate = Pedido.calcularFechaEstimadaEntrega(shipmentDate);

        assertEquals(LocalDate.of(2026, 11, 4), estimatedDeliveryDate);
    }

    @Test
    void shouldKeepPaymentStatusIndependentFromFulfillmentStatus() {
        Pedido pedido = new Pedido(
                "cliente-1",
                List.of(),
                BigDecimal.ZERO,
                new DireccionEnvioPedido("direccion-1", "Cliente", "Calle 1", null,
                        "13001", "Ciudad Real", "Ciudad Real", null),
                new MetodoPagoPedido("pago-1", PaymentMethodType.CARD, "1234"),
                PedidoEstado.PENDIENTE,
                PedidoEstadoPago.PAGADO);

        assertEquals(PedidoEstado.PENDIENTE, pedido.getEstado());
        assertEquals(PedidoEstadoPago.PAGADO, pedido.getEstadoPago());

        pedido.setEstado(PedidoEstado.PREPARANDO);

        assertEquals(PedidoEstadoPago.PAGADO, pedido.getEstadoPago());
    }

    @Test
    void shouldTreatLegacyOrdersWithoutPaymentStatusAsPaid() {
        Pedido pedido = new Pedido(
                "cliente-1",
                List.of(),
                BigDecimal.ZERO,
                new DireccionEnvioPedido("direccion-1", "Cliente", "Calle 1", null,
                        "13001", "Ciudad Real", "Ciudad Real", null),
                new MetodoPagoPedido("pago-1", PaymentMethodType.CARD, "1234"),
                PedidoEstado.PENDIENTE);

        org.springframework.test.util.ReflectionTestUtils.setField(pedido, "estadoPago", null);

        assertEquals(PedidoEstadoPago.PAGADO, pedido.getEstadoPago());
    }
}
