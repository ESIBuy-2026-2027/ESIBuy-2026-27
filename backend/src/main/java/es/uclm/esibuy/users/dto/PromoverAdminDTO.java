package es.uclm.esibuy.users.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PromoverAdminDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @JsonAlias({"firstName", "nombre"})
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    @JsonAlias({"lastName", "apellidos"})
    private String apellidos;

    @JsonAlias({"email"})
    private String email;

    @Size(max = 100, message = "La sede no puede superar los 100 caracteres")
    @JsonAlias({"headquarters", "sede"})
    private String sede;

    @Size(max = 500, message = "La imagen de perfil no puede superar los 500 caracteres")
    @JsonAlias({"profileImage", "imagenPerfil"})
    private String imagenPerfil;

    @JsonAlias({"hireDate", "fechaIncorporacion"})
    private String fechaIncorporacion;

    public PromoverAdminDTO() {
    }

    public PromoverAdminDTO(String nombre, String apellidos, String sede, String imagenPerfil) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.sede = sede;
        this.imagenPerfil = imagenPerfil;
    }

    @JsonProperty("nombre")
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @JsonProperty("apellidos")
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

    @JsonProperty("sede")
    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    @JsonProperty("imagenPerfil")
    public String getImagenPerfil() {
        return imagenPerfil;
    }

    public void setImagenPerfil(String imagenPerfil) {
        this.imagenPerfil = imagenPerfil;
    }

    @JsonProperty("fechaIncorporacion")
    public String getFechaIncorporacion() {
        return fechaIncorporacion;
    }

    public void setFechaIncorporacion(String fechaIncorporacion) {
        this.fechaIncorporacion = fechaIncorporacion;
    }
}
