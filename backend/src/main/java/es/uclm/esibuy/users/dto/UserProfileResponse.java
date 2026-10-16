
package es.uclm.esibuy.users.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;

public record UserProfileResponse(
        String firstName,
        String lastName,
        String email,
        String phone,
        String maskedNationalId,
        String profileImage,
        CustomerType customerType,
        LocalDate birthDate,
        String businessName,
        String defaultCategory,
        String headquarters,
        LocalDateTime hireDate) {

    private static final int VISIBLE_NATIONAL_ID_CHARS = 4;

    public static UserProfileResponse from(User user) {
        CustomerType customerType = null;
        LocalDate birthDate = null;
        String businessName = null;
        String defaultCategory = null;
        String headquarters = null;
        LocalDateTime hireDate = null;

        if (user.getRole() == UserRole.CLIENT) {
            customerType = user.getClientProfile() == null
                    || user.getClientProfile().getCustomerType() == null
                            ? CustomerType.STANDARD
                            : user.getClientProfile().getCustomerType();

            birthDate = user.getClientProfile() == null
                    ? null
                    : user.getClientProfile().getBirthDate();
        }

        if (user.getRole() == UserRole.SELLER
                && user.getSellerProfile() != null) {
            businessName = user.getSellerProfile().getBusinessName();
            defaultCategory = user.getSellerProfile().getDefaultCategory();
        }

        if (user.getRole() == UserRole.ADMIN
                && user.getAdminProfile() != null) {
            headquarters = user.getAdminProfile().getHeadquarters();
            hireDate = user.getAdminProfile().getHireDate();
        }

        return new UserProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                maskNationalId(user.getNationalId()),
                user.getProfileImage(),
                customerType,
                birthDate,
                businessName,
                defaultCategory,
                headquarters,
                hireDate);
    }

    static String maskNationalId(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            return null;
        }

        String value = nationalId.trim();

        if (value.length() <= VISIBLE_NATIONAL_ID_CHARS) {
            return "*".repeat(value.length());
        }

        int hidden = value.length() - VISIBLE_NATIONAL_ID_CHARS;

        return "*".repeat(hidden) + value.substring(hidden);
    }
}
