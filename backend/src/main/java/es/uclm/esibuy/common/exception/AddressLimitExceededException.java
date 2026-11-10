package es.uclm.esibuy.common.exception;

public class AddressLimitExceededException extends RuntimeException {

    public AddressLimitExceededException(String message) {
        super(message);
    }
}
