package es.uclm.esibuy.users.model;

import java.time.LocalDate;

public class ClientProfile {

    private LocalDate birthDate;
    private CustomerType customerType = CustomerType.STANDARD;

    public ClientProfile() {
    }

    public ClientProfile(LocalDate birthDate, CustomerType customerType) {
        this.birthDate = birthDate;
        this.customerType = customerType != null ? customerType : CustomerType.STANDARD;
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
