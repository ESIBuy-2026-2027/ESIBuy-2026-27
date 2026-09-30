package es.uclm.esibuy.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.uclm.esibuy.users.model.User;

public class CandidatoDTO {

    private String id;
    private String nombre;
    private String apellidos;
    private String email;
    private String rol;
    private boolean activo;
    private String imagenPerfil;
    private String sede;

    public CandidatoDTO() {
    }

    public CandidatoDTO(String id, String nombre, String apellidos, String email, String rol, boolean activo, String imagenPerfil, String sede) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
        this.imagenPerfil = imagenPerfil;
        this.sede = sede;
    }

    public static CandidatoDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        String sede = user.getAdminProfile() != null ? user.getAdminProfile().getHeadquarters() : null;
        return new CandidatoDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.isActive(),
                user.getProfileImage(),
                sede
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @JsonProperty("rol")
    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    @JsonProperty("activo")
    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @JsonProperty("imagenPerfil")
    public String getImagenPerfil() {
        return imagenPerfil;
    }

    public void setImagenPerfil(String imagenPerfil) {
        this.imagenPerfil = imagenPerfil;
    }

    @JsonProperty("sede")
    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }
}
