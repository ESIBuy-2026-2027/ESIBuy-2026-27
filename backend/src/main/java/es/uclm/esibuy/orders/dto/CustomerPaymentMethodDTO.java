package es.uclm.esibuy.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.uclm.esibuy.users.model.PaymentMethodType;

public record CustomerPaymentMethodDTO(
                @JsonProperty("tipo") PaymentMethodType type,

                String lastFourDigits) {
}