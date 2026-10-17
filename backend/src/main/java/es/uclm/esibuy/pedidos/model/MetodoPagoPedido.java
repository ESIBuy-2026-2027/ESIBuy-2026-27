package es.uclm.esibuy.pedidos.model;

import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.PaymentMethodType;

public record MetodoPagoPedido(
        String metodoPagoId,
        PaymentMethodType tipo,
        String lastFourDigits) {

    public static MetodoPagoPedido from(PaymentMethod method) {
        return new MetodoPagoPedido(
                method.getId(),
                method.getType(),
                method.getLastFourDigits());
    }
}
