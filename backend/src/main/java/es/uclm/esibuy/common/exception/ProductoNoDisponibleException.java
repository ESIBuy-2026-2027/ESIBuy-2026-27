package es.uclm.esibuy.common.exception;

public class ProductoNoDisponibleException extends IllegalStateException {

    public ProductoNoDisponibleException() {
        super("El producto no está disponible");
    }
}
