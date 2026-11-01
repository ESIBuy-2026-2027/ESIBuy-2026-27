package es.uclm.esibuy.security.password;

public class CompromisedPasswordException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CompromisedPasswordException() {
        super("La contraseña elegida aparece en filtraciones conocidas. Elige otra contraseña.");
    }
}