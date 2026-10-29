package es.uclm.esibuy.pedidos.model;

import java.time.LocalDateTime;

public record EstadoPedidoHistorial(PedidoEstado estado, LocalDateTime fecha) {
}
