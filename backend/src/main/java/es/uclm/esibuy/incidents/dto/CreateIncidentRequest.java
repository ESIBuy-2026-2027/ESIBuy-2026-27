package es.uclm.esibuy.incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateIncidentRequest {

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 150, message = "El asunto no puede superar los 150 caracteres")
    private String subject;

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    @Size(max = 100, message = "La referencia no puede superar los 100 caracteres")
    private String reference;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 3000, message = "La descripción no puede superar los 3000 caracteres")
    private String description;

    public CreateIncidentRequest() {
    }

    public CreateIncidentRequest(String subject, String category, String reference, String description) {
        this.subject = subject;
        this.category = category;
        this.reference = reference;
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

