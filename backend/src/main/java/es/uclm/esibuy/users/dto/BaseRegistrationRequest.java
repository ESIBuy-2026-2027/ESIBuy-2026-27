package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public abstract class BaseRegistrationRequest extends BaseUserRequest {

    @Override
    @Email(message = "El formato de email no es válido")
    public String getEmail() {
        return super.getEmail();
    }

    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(max = 20, message = "El documento de identidad no puede superar los 20 caracteres")
    private String nationalId;

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

}
