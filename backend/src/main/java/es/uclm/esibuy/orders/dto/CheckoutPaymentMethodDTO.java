package es.uclm.esibuy.orders.dto;

import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.PaymentMethodType;

public record CheckoutPaymentMethodDTO(
        String id,
        PaymentMethodType type,
        String cardBrand,
        String lastFourDigits,
        boolean defaultMethod) {

    public static CheckoutPaymentMethodDTO from(PaymentMethod method) {
        return new CheckoutPaymentMethodDTO(
                method.getId(),
                method.getType(),
                method.getCardBrand(),
                method.getLastFourDigits(),
                method.isDefaultMethod());
    }
}