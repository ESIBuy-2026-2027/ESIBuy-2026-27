package es.uclm.esibuy.pedidos.dto;

import java.time.LocalDate;
import java.util.List;

public record SeguimientoEnvioDTO(
        List<EstadoPedidoHistorialDTO> historialEstados,
        LocalDate estimatedDeliveryDate) {
}
