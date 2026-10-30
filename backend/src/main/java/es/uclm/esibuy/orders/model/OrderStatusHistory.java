package es.uclm.esibuy.orders.model;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Field;

public record OrderStatusHistory(
                @Field("estado") OrderStatus status,
                @Field("fecha") LocalDateTime date) {
}