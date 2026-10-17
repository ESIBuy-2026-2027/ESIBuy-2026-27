package es.uclm.esibuy.pedidos.service;

import java.math.BigDecimal;

import es.uclm.esibuy.pedidos.model.MetodoPagoPedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimuladorPasarelaPago implements PasarelaPago {

    private final boolean simularFallo;

    public SimuladorPasarelaPago(@Value("${esibuy.pagos.simular-fallo:false}") boolean simularFallo) {
        this.simularFallo = simularFallo;
    }

    @Override
    public boolean cobrar(MetodoPagoPedido metodoPago, BigDecimal importe) {
        return !simularFallo;
    }
}
