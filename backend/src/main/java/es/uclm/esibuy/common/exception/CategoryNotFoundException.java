package es.uclm.esibuy.common.exception;

import java.util.List;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(List<String> categoryIds) {
        super("No existen las categorías indicadas: " + String.join(", ", categoryIds));
    }
}
