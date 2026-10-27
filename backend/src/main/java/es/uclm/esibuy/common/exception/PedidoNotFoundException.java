package es.uclm.esibuy.common.exception;

public class PedidoNotFoundException extends RuntimeException {

    public PedidoNotFoundException() {
        super("El pedido no existe");
    }
}
