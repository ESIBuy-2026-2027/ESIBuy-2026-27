package es.uclm.esibuy.common.exception;

public class CategoryIdNotFoundException extends RuntimeException {

    public CategoryIdNotFoundException() {
        super("La categoría ya no existe");
    }
}
