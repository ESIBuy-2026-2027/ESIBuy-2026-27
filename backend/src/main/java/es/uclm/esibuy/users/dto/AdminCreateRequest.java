package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class AdminCreateRequest extends BaseUserRequest {

    @Size(max = 100, message = "La sede no puede superar los 100 caracteres")
    private String headquarters;

    @Override
    @Email(message = "El formato de email no es válido (ejemplo: user@example.com)")
    public String getEmail() {
        return super.getEmail();
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = headquarters;
    }

}
