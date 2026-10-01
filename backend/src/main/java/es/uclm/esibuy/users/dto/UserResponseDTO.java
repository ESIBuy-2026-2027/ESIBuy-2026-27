package es.uclm.esibuy.users.dto;

public class UserResponseDTO {

    private final String id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;

    public UserResponseDTO(String id, String email, String firstName, String lastName, String phone) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
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
}