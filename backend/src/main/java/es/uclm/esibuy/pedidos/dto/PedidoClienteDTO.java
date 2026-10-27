package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import es.uclm.esibuy.pedidos.model.PedidoEstado;

public record PedidoClienteDTO(
        String pedidoId,
        LocalDateTime fechaCreacion,
        List<ItemPedidoClienteDTO> items,
        BigDecimal total,
        PedidoEstado estado,
        DireccionEnvioClienteDTO direccionEnvio,
        MetodoPagoClienteDTO metodoPago) {

    public PedidoClienteDTO(
            String pedidoId,
            LocalDateTime fechaCreacion,
            List<ItemPedidoClienteDTO> items,
            BigDecimal total,
            PedidoEstado estado) {
        this(pedidoId, fechaCreacion, items, total, estado, null, null);
    }
}
