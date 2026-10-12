package es.uclm.esibuy.users.dto;

import java.time.LocalDate;
import java.time.Period;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 25, message = "El nombre debe tener entre 2 y 25 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} '\\-]*$", message = "El nombre contiene caracteres no permitidos")
    private String firstName;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 50, message = "Los apellidos deben tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} '\\-]*$", message = "Los apellidos contienen caracteres no permitidos")
    private String lastName;

    @Size(max = 20, message = "El documento de identidad no puede superar los 20 caracteres")
    private String nationalId;

    @Pattern(regexp = "^$|^\\+?\\d{9}$", message = "El teléfono debe contener exactamente 9 dígitos numéricos")
    private String phone;

    @Size(max = 500, message = "La imagen de perfil no puede superar los 500 caracteres")
    private String profileImage;

    @Size(max = 100, message = "La sede no puede superar los 100 caracteres")
    private String headquarters;

    @Size(max = 100, message = "El nombre comercial no puede superar los 100 caracteres")
    private String businessName;

    @Past(message = "La fecha de nacimiento debe ser anterior a la actual")
    private LocalDate birthDate;

    @Size(max = 50, message = "La categoría por defecto no puede superar los 50 caracteres")
    private String defaultCategory;

    @AssertTrue(message = "El cliente debe ser mayor de edad")
    public boolean isAdult() {
        return birthDate == null || Period.between(birthDate, LocalDate.now()).getYears() >= 18;
    }

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

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = normalize(nationalId);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = normalize(phone);
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = normalize(profileImage);
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = normalize(headquarters);
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = normalize(businessName);
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = normalize(defaultCategory);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}