package es.uclm.esibuy.common.exception;

public class UserDeletionConflictException extends RuntimeException {

    public UserDeletionConflictException() {
        super("Solo se pueden eliminar cuentas de otros administradores");
    }

    public UserDeletionConflictException(String message) {
        super(message);
    }
}