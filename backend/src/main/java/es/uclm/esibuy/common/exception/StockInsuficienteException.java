package es.uclm.esibuy.common.exception;

public class StockInsuficienteException extends IllegalStateException {

    public StockInsuficienteException() {
        super("No hay stock suficiente para la cantidad solicitada");
    }
}
