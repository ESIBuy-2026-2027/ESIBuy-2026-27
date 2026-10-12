package es.uclm.esibuy.users.dto;

import java.time.LocalDate;

public class UserResponseDTO {

    private final String id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String role;
    private final String profileImage;
    private final String nationalId;
    private final String headquarters;
    private final String businessName;
    private final LocalDate birthDate;
    private final String defaultCategory;

    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone, String role) {
        this(id, email, firstName, lastName, phone, role, null, null, null, null, null, null);
    }

    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone, String role,
            String profileImage) {
        this(id, email, firstName, lastName, phone, role, profileImage, null, null, null, null, null);
    }

    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone, String role,
            String profileImage, String nationalId, String headquarters, String businessName) {
        this(id, email, firstName, lastName, phone, role, profileImage, nationalId, headquarters, businessName, null,
                null);
    }

    @SuppressWarnings("java:S107")
    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone, String role,
            String profileImage, String nationalId, String headquarters, String businessName,
            LocalDate birthDate, String defaultCategory) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
        this.profileImage = profileImage;
        this.nationalId = nationalId;
        this.headquarters = headquarters;
        this.businessName = businessName;
        this.birthDate = birthDate;
        this.defaultCategory = defaultCategory;
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

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public String getBusinessName() {
        return businessName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }
}