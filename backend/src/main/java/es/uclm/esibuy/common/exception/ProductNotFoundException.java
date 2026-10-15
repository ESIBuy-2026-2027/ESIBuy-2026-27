package es.uclm.esibuy.common.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException() {
        super("Producto no encontrado");
    }
}
