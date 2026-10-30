package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import es.uclm.esibuy.pedidos.model.PedidoEstado;
import es.uclm.esibuy.pedidos.model.PedidoEstadoPago;

public record PedidoClienteDTO(
        String pedidoId,
        LocalDateTime fechaCreacion,
        List<ItemPedidoClienteDTO> items,
        BigDecimal total,
        PedidoEstado estado,
        DireccionEnvioClienteDTO direccionEnvio,
        MetodoPagoClienteDTO metodoPago,
        PedidoEstadoPago estadoPago) {

    public PedidoClienteDTO(
            String pedidoId,
            LocalDateTime fechaCreacion,
            List<ItemPedidoClienteDTO> items,
            BigDecimal total,
            PedidoEstado estado,
            DireccionEnvioClienteDTO direccionEnvio,
            MetodoPagoClienteDTO metodoPago) {
        this(
                pedidoId,
                fechaCreacion,
                items,
                total,
                estado,
                direccionEnvio,
                metodoPago,
                PedidoEstadoPago.PAGADO);
    }

    public PedidoClienteDTO(
            String pedidoId,
            LocalDateTime fechaCreacion,
            List<ItemPedidoClienteDTO> items,
            BigDecimal total,
            PedidoEstado estado) {
        this(pedidoId, fechaCreacion, items, total, estado, null, null, PedidoEstadoPago.PAGADO);
    }
}
