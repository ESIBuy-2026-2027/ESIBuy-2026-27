package es.uclm.esibuy.users.dto;

import java.time.LocalDate;
import java.time.Period;

import es.uclm.esibuy.users.model.CustomerType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public class ClientRegistrationRequest extends BaseRegistrationRequest {

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate birthDate;

    private CustomerType customerType = CustomerType.STANDARD;

    @AssertTrue(message = "El cliente debe ser mayor de edad")
    public boolean isAdult() {
        return birthDate != null && Period.between(birthDate, LocalDate.now()).getYears() >= 18;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }
}
