package es.uclm.esibuy.pedidos.service;

import java.math.BigDecimal;

import es.uclm.esibuy.pedidos.model.MetodoPagoPedido;

public interface PasarelaPago {

    boolean cobrar(MetodoPagoPedido metodoPago, BigDecimal importe);
}
