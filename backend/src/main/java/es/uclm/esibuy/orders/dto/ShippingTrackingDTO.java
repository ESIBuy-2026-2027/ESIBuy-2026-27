package es.uclm.esibuy.orders.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShippingTrackingDTO(
        @JsonProperty("historialEstados") List<OrderStatusHistoryDTO> statusHistory,

        LocalDate estimatedDeliveryDate) {
}