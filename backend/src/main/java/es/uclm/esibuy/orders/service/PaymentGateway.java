package es.uclm.esibuy.orders.service;

import java.math.BigDecimal;

import es.uclm.esibuy.orders.model.OrderPaymentMethod;

public interface PaymentGateway {

    boolean charge(OrderPaymentMethod paymentMethod, BigDecimal amount);
}