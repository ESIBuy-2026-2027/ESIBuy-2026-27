package es.uclm.esibuy.pedidos.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearPedidoDTO(
        @NotBlank(message = "La dirección de envío es obligatoria") String direccionId,
        @NotBlank(message = "El método de pago es obligatorio") String metodoPagoId,
        String vendedorId) {

    public CrearPedidoDTO(String direccionId, String metodoPagoId) {
        this(direccionId, metodoPagoId, null);
    }
}
