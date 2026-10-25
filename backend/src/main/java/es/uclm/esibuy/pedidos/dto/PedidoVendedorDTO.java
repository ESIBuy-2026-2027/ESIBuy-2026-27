package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import es.uclm.esibuy.pedidos.model.PedidoEstado;

public record PedidoVendedorDTO(
        String pedidoId,
        LocalDateTime fechaCreacion,
        String clienteNombre,
        String clienteEmail,
        String direccionEnvio,
        List<ItemPedidoVendedorDTO> items,
        BigDecimal total,
        PedidoEstado estado) {
}
