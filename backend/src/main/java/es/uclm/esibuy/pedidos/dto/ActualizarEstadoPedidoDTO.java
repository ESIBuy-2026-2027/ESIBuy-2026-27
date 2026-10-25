package es.uclm.esibuy.pedidos.dto;

import es.uclm.esibuy.pedidos.model.PedidoEstado;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoPedidoDTO(
        @NotNull PedidoEstado estado,
        String justificacion) {
}
