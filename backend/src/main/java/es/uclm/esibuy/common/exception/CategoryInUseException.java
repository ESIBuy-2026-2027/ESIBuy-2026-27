package es.uclm.esibuy.common.exception;

public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException() {
        super("La categoría está en uso por productos y no se puede eliminar");
    }
}
