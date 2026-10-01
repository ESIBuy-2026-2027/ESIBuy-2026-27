package es.uclm.esibuy.security;

import java.io.Serializable;

import es.uclm.esibuy.users.model.UserRole;
import org.springframework.security.core.AuthenticatedPrincipal;

public record AuthenticatedUserPrincipal(
        String id,
        String email,
        UserRole role,
        boolean active) implements AuthenticatedPrincipal, Serializable {

    @Override
    public String getName() {
        return email;
    }
}
