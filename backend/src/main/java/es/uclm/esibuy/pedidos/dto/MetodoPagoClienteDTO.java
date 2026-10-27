package es.uclm.esibuy.pedidos.dto;

import es.uclm.esibuy.users.model.PaymentMethodType;

public record MetodoPagoClienteDTO(
        PaymentMethodType tipo,
        String lastFourDigits) {
}
