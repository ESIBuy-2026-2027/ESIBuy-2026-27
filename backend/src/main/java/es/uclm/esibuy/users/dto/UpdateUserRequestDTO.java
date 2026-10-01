package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 25, message = "El nombre debe tener entre 2 y 25 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} '\\-]*$", message = "El nombre contiene caracteres no permitidos")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} '\\-]*$", message = "Los apellidos contienen caracteres no permitidos")
    private String lastName;

    @Size(max = 10, message = "El teléfono no puede superar los 10 caracteres")
    @Pattern(regexp = "^$|^\\+?\\d{9}$", message = "El teléfono debe contener exactamente 9 dígitos numéricos")
    private String phone;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = normalize(firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = normalize(lastName);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = normalize(phone);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}