package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import es.uclm.esibuy.pedidos.model.PedidoEstado;
import es.uclm.esibuy.pedidos.model.PedidoEstadoPago;

public record PedidoConfirmadoDTO(
        String pedidoId,
        BigDecimal precioTotal,
        PedidoEstado estado,
        LocalDateTime fechaCreacion,
        PedidoEstadoPago estadoPago) {

    public PedidoConfirmadoDTO(
            String pedidoId,
            BigDecimal precioTotal,
            PedidoEstado estado,
            LocalDateTime fechaCreacion) {
        this(
                pedidoId,
                precioTotal,
                estado,
                fechaCreacion,
                PedidoEstadoPago.PAGADO);
    }
}
