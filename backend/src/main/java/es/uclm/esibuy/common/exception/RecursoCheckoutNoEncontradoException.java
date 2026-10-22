package es.uclm.esibuy.common.exception;

public class RecursoCheckoutNoEncontradoException extends RuntimeException {

    public RecursoCheckoutNoEncontradoException() {
        super("No se encontró la dirección o el método de pago seleccionado");
    }
}
