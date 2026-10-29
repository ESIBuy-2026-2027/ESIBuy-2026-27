package es.uclm.esibuy.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException() {
        super("Incidencia no encontrada");
    }

    public IncidentNotFoundException(String id) {
        super("Incidencia no encontrada: " + id);
    }
}

