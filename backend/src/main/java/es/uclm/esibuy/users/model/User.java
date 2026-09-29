package es.uclm.esibuy.users.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String nationalId;
    private String phone;
    private String passwordHash;
    private String profileImage;
    private UserRole role;
    private CustomerType customerType;
    private String defaultCategory;
    private String businessName;
    private boolean active;
    private Instant createdAt;

    protected User() {
    }

    public User(String email, String firstName, String lastName, UserRole role) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.active = role == UserRole.ADMIN;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public UserRole getRole() {
        return role;
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

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
}
