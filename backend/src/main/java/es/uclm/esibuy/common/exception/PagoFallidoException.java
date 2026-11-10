package es.uclm.esibuy.common.exception;

public class PagoFallidoException extends RuntimeException {

    public PagoFallidoException() {
        super("El pago fue rechazado. No se ha realizado ningún cargo ni modificado tu carrito");
    }
}
