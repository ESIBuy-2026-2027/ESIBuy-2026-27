package es.uclm.esibuy.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.PaymentMethodType;

public record PaymentMethodResponse(
        String id,
        PaymentMethodType type,
        String label,
        String cardholderName,
        String cardBrand,
        String lastFourDigits,
        Integer expirationMonth,
        Integer expirationYear,
        String paypalEmail,
        String bizumPhone,
        @JsonProperty("isDefault") boolean defaultMethod) {

    public static PaymentMethodResponse from(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.getId(),
                method.getType(),
                method.getLabel(),
                method.getCardholderName(),
                method.getCardBrand(),
                method.getLastFourDigits(),
                method.getExpirationMonth(),
                method.getExpirationYear(),
                method.getPaypalEmail(),
                method.getBizumPhone(),
                method.isDefaultMethod());
    }
}