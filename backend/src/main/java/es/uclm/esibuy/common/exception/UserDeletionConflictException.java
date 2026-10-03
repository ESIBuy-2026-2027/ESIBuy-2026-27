package es.uclm.esibuy.common.exception;

public class UserDeletionConflictException extends RuntimeException {

    public UserDeletionConflictException() {
        super("No se puede eliminar el vendedor mientras tenga productos asociados");
    }
}