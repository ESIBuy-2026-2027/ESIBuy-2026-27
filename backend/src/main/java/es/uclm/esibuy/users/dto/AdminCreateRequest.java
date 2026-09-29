package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es valido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    private String email;

    @Size(max = 100, message = "La sede no puede superar los 100 caracteres")
    private String headquarters;

    @Size(max = 20)
    @Pattern(regexp = "^$|^[0-9]{9}$", message = "El telefono debe contener exactamente 9 digitos numericos")
    private String phone;

    @Size(max = 500, message = "La imagen de perfil no puede superar los 500 caracteres")
    private String profileImage;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 12, max = 128, message = "La contrasena debe tener entre 12 y 128 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$", message = "La contrasena debe tener mayuscula, minuscula, numero y caracter especial")
    private String password;

    @NotBlank(message = "Debe repetir la contrasena")
    private String repeatedPassword;

    @AssertTrue(message = "Las contrasenas no coinciden")
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
