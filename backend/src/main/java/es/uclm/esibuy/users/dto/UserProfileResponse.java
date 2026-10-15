package es.uclm.esibuy.users.dto;

import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;

/**
 * Datos del perfil que el propio usuario puede consultar (HU 151).
 * No incluye hash de contraseña, secreto TOTP ni códigos de respaldo,
 * y el DNI se devuelve enmascarado.
 */
public record UserProfileResponse(
        String firstName,
        String lastName,
        String email,
        String phone,
        String maskedNationalId,
        String profileImage,
        CustomerType customerType,
        String businessName,
        String defaultCategory) {

    private static final int VISIBLE_NATIONAL_ID_CHARS = 4;

    public static UserProfileResponse from(User user) {
        CustomerType customerType = null;
        String businessName = null;
        String defaultCategory = null;

        if (user.getRole() == UserRole.CLIENT) {
            customerType = user.getClientProfile() == null
                    || user.getClientProfile().getCustomerType() == null
                            ? CustomerType.STANDARD
                            : user.getClientProfile().getCustomerType();
        }

        if (user.getRole() == UserRole.SELLER
                && user.getSellerProfile() != null) {
            businessName = user.getSellerProfile().getBusinessName();
            defaultCategory = user.getSellerProfile().getDefaultCategory();
        }

        return new UserProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                maskNationalId(user.getNationalId()),
                user.getProfileImage(),
                customerType,
                businessName,
                defaultCategory);
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

        return "*".repeat(hidden)
                + value.substring(hidden);
    }
}