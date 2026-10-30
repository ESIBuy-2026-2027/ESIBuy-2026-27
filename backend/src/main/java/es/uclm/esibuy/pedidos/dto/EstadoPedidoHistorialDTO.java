package es.uclm.esibuy.pedidos.dto;

import java.time.LocalDateTime;

import es.uclm.esibuy.pedidos.model.PedidoEstado;

public record EstadoPedidoHistorialDTO(PedidoEstado estado, LocalDateTime fecha) {
}
