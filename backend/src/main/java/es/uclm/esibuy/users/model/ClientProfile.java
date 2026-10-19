package es.uclm.esibuy.users.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClientProfile {

    private LocalDate birthDate;
    private CustomerType customerType = CustomerType.STANDARD;
    private List<ShippingAddress> addresses = new ArrayList<>();
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

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

    public List<ShippingAddress> getAddresses() {
        return addresses == null ? new ArrayList<>() : addresses;
    }

    public void setAddresses(List<ShippingAddress> addresses) {
        this.addresses = addresses;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods == null ? new ArrayList<>() : paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }
}