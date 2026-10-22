package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AltaAdminDTO {

    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(max = 20, message = "El documento de identidad no puede superar los 20 caracteres")
    private String nationalId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 25, message = "El nombre debe tener entre 2 y 25 caracteres")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 50, message = "Los apellidos deben tener entre 2 y 50 caracteres")
    private String lastName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 128, message = "La contraseña debe tener entre 12 y 128 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$", message = "La contraseña debe tener mayúscula, minúscula, número y caracter especial")
    private String password;

    @NotBlank(message = "Debe repetir la contraseña")
    private String confirmPassword;

    @Size(max = 100, message = "La sede no puede superar los 100 caracteres")
    private String headquarters;

    @Size(max = 500, message = "La imagen de perfil no puede superar los 500 caracteres")
    private String profileImage;

    public AltaAdminDTO() {
    }

    public AltaAdminDTO(String nationalId, String firstName, String lastName, String email,
            String password, String confirmPassword, String headquarters, String profileImage) {
        this.nationalId = nationalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.headquarters = headquarters;
        this.profileImage = profileImage;
    }

    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isPasswordsMatch() {
        if (password == null || confirmPassword == null) {
            return true;
        }

        return password.equals(confirmPassword);
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = headquarters;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
