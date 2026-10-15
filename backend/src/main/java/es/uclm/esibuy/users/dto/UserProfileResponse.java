package es.uclm.esibuy.users.dto;

import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.model.User;

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
        CustomerType customerType) {

    private static final int VISIBLE_NATIONAL_ID_CHARS = 4;

    public static UserProfileResponse from(User user) {
        CustomerType customerType = user.getClientProfile() == null
                || user.getClientProfile().getCustomerType() == null
                        ? CustomerType.STANDARD
                        : user.getClientProfile().getCustomerType();
        return new UserProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                maskNationalId(user.getNationalId()),
                user.getProfileImage(),
                customerType);
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