package es.uclm.esibuy.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.uclm.esibuy.orders.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusDTO(
        @NotNull @JsonProperty("estado") OrderStatus status,

        @JsonProperty("justificacion") String justification) {
}