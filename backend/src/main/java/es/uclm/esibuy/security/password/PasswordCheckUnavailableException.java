package es.uclm.esibuy.security.password;

public class PasswordCheckUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PasswordCheckUnavailableException() {
        super("No se ha podido comprobar la seguridad de la contraseña. Inténtalo de nuevo más tarde.");
    }

    public PasswordCheckUnavailableException(Throwable cause) {
        super("No se ha podido comprobar la seguridad de la contraseña. Inténtalo de nuevo más tarde.",
                cause);
    }
}