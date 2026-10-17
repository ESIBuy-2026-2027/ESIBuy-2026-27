package es.uclm.esibuy.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

import es.uclm.esibuy.cart.dto.ItemCarritoDTO;
import es.uclm.esibuy.users.dto.ShippingAddressResponse;

public record CheckoutResumenDTO(
        List<ShippingAddressResponse> direcciones,
        List<MetodoPagoCheckoutDTO> metodosPago,
        List<ItemCarritoDTO> items,
        BigDecimal precioTotal) {

    public CheckoutResumenDTO {
        direcciones = List.copyOf(direcciones);
        metodosPago = List.copyOf(metodosPago);
        items = List.copyOf(items);
    }
}
