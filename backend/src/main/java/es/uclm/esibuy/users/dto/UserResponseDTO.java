package es.uclm.esibuy.users.dto;

public class UserResponseDTO {

    private final String id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String role;

    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone, String role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
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
}