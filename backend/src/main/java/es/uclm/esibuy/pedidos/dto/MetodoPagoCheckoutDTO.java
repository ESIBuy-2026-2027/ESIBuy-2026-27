package es.uclm.esibuy.pedidos.dto;

import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.PaymentMethodType;

public record MetodoPagoCheckoutDTO(
        String id,
        PaymentMethodType type,
        String cardBrand,
        String lastFourDigits,
        boolean defaultMethod) {

    public static MetodoPagoCheckoutDTO from(PaymentMethod method) {
        return new MetodoPagoCheckoutDTO(
                method.getId(),
                method.getType(),
                method.getCardBrand(),
                method.getLastFourDigits(),
                method.isDefaultMethod());
    }
}
