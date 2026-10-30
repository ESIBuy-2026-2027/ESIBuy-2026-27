package es.uclm.esibuy.orders.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import es.uclm.esibuy.orders.model.OrderPaymentMethod;

@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final boolean simulateFailure;

    public SimulatedPaymentGateway(
            @Value("${esibuy.pagos.simular-fallo:false}") boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public boolean charge(OrderPaymentMethod paymentMethod, BigDecimal amount) {
        return !simulateFailure;
    }
}