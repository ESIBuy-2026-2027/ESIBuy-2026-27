package es.uclm.esibuy.users.dto;

public class UserResponseDTO {

    private final String id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String role;
    private final String profileImage;

    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone, String role) {
        this(id, email, firstName, lastName, phone, role, null);
    }

    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone, String role, String profileImage) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
        this.profileImage = profileImage;
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
}