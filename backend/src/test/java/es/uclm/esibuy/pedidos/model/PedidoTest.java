package es.uclm.esibuy.pedidos.model;

import java.time.LocalDate;

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
}
