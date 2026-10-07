package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AltaAdminDTO {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}[A-Za-z]$", message = "El DNI debe tener 8 dígitos y una letra")
    private String dni;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 25, message = "El nombre debe tener entre 2 y 25 caracteres")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 50, message = "Los apellidos deben tener entre 2 y 50 caracteres")
    private String apellidos;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 128, message = "La contraseña debe tener entre 12 y 128 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S+$",
        message = "La contraseña debe tener mayúscula, minúscula, número y caracter especial"
    )
    private String contrasena;

    @NotBlank(message = "Debe repetir la contraseña")
    private String repetirContrasena;

    @Size(max = 100, message = "La sede no puede superar los 100 caracteres")
    private String sede;

    @Size(max = 500, message = "La imagen de perfil no puede superar los 500 caracteres")
    private String imagenPerfil;

    public AltaAdminDTO() {
    }

    public AltaAdminDTO(String dni, String nombre, String apellidos, String email,
                        String contrasena, String repetirContrasena, String sede, String imagenPerfil) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.contrasena = contrasena;
        this.repetirContrasena = repetirContrasena;
        this.sede = sede;
        this.imagenPerfil = imagenPerfil;
    }

    @AssertTrue(message = "Las contraseñas no coinciden")
    public boolean isContrasenasCoincidentes() {
        return contrasena != null && contrasena.equals(repetirContrasena);
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRepetirContrasena() {
        return repetirContrasena;
    }

    public void setRepetirContrasena(String repetirContrasena) {
        this.repetirContrasena = repetirContrasena;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    public String getImagenPerfil() {
        return imagenPerfil;
    }

    public void setImagenPerfil(String imagenPerfil) {
        this.imagenPerfil = imagenPerfil;
    }
}
