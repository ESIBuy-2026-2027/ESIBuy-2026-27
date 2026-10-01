package es.uclm.esibuy.users.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;

public record AdminUserResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String nationalId,
        String phone,
        String profileImage,
        UserRole role,
        boolean active,
        Instant createdAt,
        ClientProfileResponse clientProfile,
        SellerProfileResponse sellerProfile,
        AdminProfileResponse adminProfile) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getNationalId(),
                user.getPhone(),
                user.getProfileImage(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getClientProfile() == null ? null : new ClientProfileResponse(
                        user.getClientProfile().getBirthDate(),
                        user.getClientProfile().getCustomerType()),
                user.getSellerProfile() == null ? null : new SellerProfileResponse(
                        user.getSellerProfile().getBusinessName(),
                        user.getSellerProfile().getDefaultCategory()),
                user.getAdminProfile() == null ? null : new AdminProfileResponse(
                        user.getAdminProfile().getHeadquarters(),
                        user.getAdminProfile().getHireDate()));
    }

    public record ClientProfileResponse(LocalDate birthDate, CustomerType customerType) {
    }

    public record SellerProfileResponse(String businessName, String defaultCategory) {
    }

    public record AdminProfileResponse(String headquarters, LocalDateTime hireDate) {
    }
}
