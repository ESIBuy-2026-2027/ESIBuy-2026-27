package es.uclm.esibuy.users.dto;

import java.time.LocalDate;
import java.time.Period;

import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Size(max = 80)
    private String firstName;
    @NotBlank
    @Size(max = 120)
    private String lastName;
    @NotNull
    private UserRole role;
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;
    @NotBlank
    @Size(max = 30)
    private String nationalId;
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
    @Size(max = 120)
    private String defaultCategory;
    @Size(max = 120)
    private String businessName;

    @AssertTrue(message = "Las contrasenas no coinciden")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(repeatedPassword);
    }

    @AssertTrue(message = "El cliente debe ser mayor de edad")
    public boolean isAdult() {
        return role != UserRole.CLIENT
                || (birthDate != null && Period.between(birthDate, LocalDate.now()).getYears() >= 18);
    }

    @AssertTrue(message = "Los datos del vendedor son obligatorios")
    public boolean isSellerDataComplete() {
        return role != UserRole.SELLER || (defaultCategory != null && !defaultCategory.isBlank() && businessName != null
                && !businessName.isBlank());
    }

    @AssertTrue(message = "El alta publica solo permite clientes y vendedores")
    public boolean isPublicRoleAllowed() {
        return role == UserRole.CLIENT || role == UserRole.SELLER;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getNationalId() {
        return nationalId;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getPassword() {
        return password;
    }

    public String getRepeatedPassword() {
        return repeatedPassword;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public String getBusinessName() {
        return businessName;
    }
}
