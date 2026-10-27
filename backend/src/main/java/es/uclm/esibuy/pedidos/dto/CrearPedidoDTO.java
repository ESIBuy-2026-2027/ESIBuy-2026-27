package es.uclm.esibuy.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CrearPedidoDTO(
        @NotBlank(message = "La dirección de envío es obligatoria") String direccionId,
        @NotBlank(message = "El método de pago es obligatorio") String metodoPagoId,
        String vendedorId,
        List<String> productoIds) {

    public CrearPedidoDTO(String direccionId, String metodoPagoId) {
        this(direccionId, metodoPagoId, null, null);
    }

    public CrearPedidoDTO(String direccionId, String metodoPagoId, String vendedorId) {
        this(direccionId, metodoPagoId, vendedorId, null);
    }
}
