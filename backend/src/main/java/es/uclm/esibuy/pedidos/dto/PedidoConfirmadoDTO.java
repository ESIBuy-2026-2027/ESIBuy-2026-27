package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import es.uclm.esibuy.pedidos.model.PedidoEstado;

public record PedidoConfirmadoDTO(
        String pedidoId,
        BigDecimal precioTotal,
        PedidoEstado estado,
        LocalDateTime fechaCreacion) {
}
