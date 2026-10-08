package es.uclm.esibuy.users.dto;

import java.time.Instant;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.SellerProfile;
import es.uclm.esibuy.users.model.User;

public class UserAdminDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String nationalId;
    private String phone;
    private String profileImage;
    private String role;
    private boolean active;
    private boolean blocked;
    private Instant createdAt;

    private ClientProfile clientProfile;
    private SellerProfile sellerProfile;
    private AdminProfile adminProfile;

    public static UserAdminDTO fromUser(User user) {
        UserAdminDTO dto = new UserAdminDTO();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.nationalId = user.getNationalId();
        dto.phone = user.getPhone();
        dto.profileImage = user.getProfileImage();
        dto.role = user.getRole() != null ? user.getRole().name() : null;
        dto.active = user.isActive();
        dto.blocked = user.isBlocked();
        dto.createdAt = user.getCreatedAt();
        dto.clientProfile = user.getClientProfile();
        dto.sellerProfile = user.getSellerProfile();
        dto.adminProfile = user.getAdminProfile();
        return dto;
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

    public String getNationalId() {
        return nationalId;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ClientProfile getClientProfile() {
        return clientProfile;
    }

    public SellerProfile getSellerProfile() {
        return sellerProfile;
    }

    public AdminProfile getAdminProfile() {
        return adminProfile;
    }
}
