package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 25, message = "El nombre debe tener entre 2 y 25 caracteres")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 50, message = "Los apellidos deben tener entre 2 y 50 caracteres")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido (ejemplo: user@example.com)")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    private String email;

    @Size(max = 100, message = "La sede no puede superar los 100 caracteres")
    private String headquarters;

    @Size(max = 20)
    @Pattern(regexp = "^$|^\\d{9}$", message = "El teléfono debe contener exactamente 9 dígitos numéricos")
    private String phone;

    @Size(max = 500, message = "La imagen de perfil no puede superar los 500 caracteres")
    private String profileImage;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 128, message = "La contraseña debe tener entre 12 y 128 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$", message = "La contraseña debe tener mayúscula, minúscula, número y caracter especial")
    private String password;

    @NotBlank(message = "Debe repetir la contraseña")
    private String repeatedPassword;

    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(repeatedPassword);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = headquarters;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepeatedPassword() {
        return repeatedPassword;
    }

    public void setRepeatedPassword(String repeatedPassword) {
        this.repeatedPassword = repeatedPassword;
    }
}
