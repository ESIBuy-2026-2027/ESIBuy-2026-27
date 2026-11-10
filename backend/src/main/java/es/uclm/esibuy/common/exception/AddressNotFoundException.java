package es.uclm.esibuy.common.exception;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException() {
        super("Dirección no encontrada");
    }
}