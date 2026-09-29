package es.uclm.esibuy.users.dto;

import java.time.LocalDate;
import java.time.Period;

import es.uclm.esibuy.users.model.CustomerType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClientRegistrationRequest {

    @NotBlank
    @Size(max = 80)
    private String firstName;

    @NotBlank
    @Size(max = 120)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 30)
    private String nationalId;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate birthDate;

    @Size(max = 20)
    @Pattern(regexp = "^$|^\\d{9}$", message = "El telefono debe contener exactamente 9 digitos")
    private String phone;

    @Size(max = 500)
    private String profileImage;

    @NotBlank
    @Size(min = 12, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$", message = "La contrasena debe tener 12 caracteres, mayuscula, minuscula, numero y simbolo")
    private String password;

    @NotBlank
    private String repeatedPassword;

    private CustomerType customerType = CustomerType.STANDARD;

    @AssertTrue(message = "Las contrasenas no coinciden")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(repeatedPassword);
    }

    @AssertTrue(message = "El cliente debe ser mayor de edad")
    public boolean isAdult() {
        return birthDate != null && Period.between(birthDate, LocalDate.now()).getYears() >= 18;
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

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
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

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }
}
