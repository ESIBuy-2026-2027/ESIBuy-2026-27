package es.uclm.esibuy.orders.model;

import org.springframework.data.mongodb.core.mapping.Field;

import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.PaymentMethodType;

public record OrderPaymentMethod(
        @Field("metodoPagoId") String paymentMethodId,
        @Field("tipo") PaymentMethodType type,
        String lastFourDigits) {

    public static OrderPaymentMethod from(PaymentMethod method) {
        return new OrderPaymentMethod(
                method.getId(),
                method.getType(),
                method.getLastFourDigits());
    }
}