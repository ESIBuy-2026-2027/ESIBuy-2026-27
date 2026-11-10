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

    private UserResponseDTO(Builder builder) {
        this.id = builder.id;
        this.email = builder.email;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.phone = builder.phone;
        this.role = builder.role;
        this.profileImage = builder.profileImage;
        this.nationalId = builder.nationalId;
        this.headquarters = builder.headquarters;
        this.businessName = builder.businessName;
        this.birthDate = builder.birthDate;
        this.defaultCategory = builder.defaultCategory;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String role;
        private String profileImage;
        private String nationalId;
        private String headquarters;
        private String businessName;
        private LocalDate birthDate;
        private String defaultCategory;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder profileImage(String profileImage) {
            this.profileImage = profileImage;
            return this;
        }

        public Builder nationalId(String nationalId) {
            this.nationalId = nationalId;
            return this;
        }

        public Builder headquarters(String headquarters) {
            this.headquarters = headquarters;
            return this;
        }

        public Builder businessName(String businessName) {
            this.businessName = businessName;
            return this;
        }

        public Builder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder defaultCategory(String defaultCategory) {
            this.defaultCategory = defaultCategory;
            return this;
        }

        public UserResponseDTO build() {
            return new UserResponseDTO(this);
        }
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