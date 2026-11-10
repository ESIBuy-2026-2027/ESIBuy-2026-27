package es.uclm.esibuy.common.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException() {
        super("Ya existe una categoría con ese nombre");
    }
}
