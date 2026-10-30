package es.uclm.esibuy.orders.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.uclm.esibuy.orders.model.OrderStatus;

public record OrderStatusHistoryDTO(
                @JsonProperty("estado") OrderStatus status,

                @JsonProperty("fecha") LocalDateTime date) {
}