
package es.uclm.esibuy.common.exception;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("El código de verificación no es válido o ha caducado");
    }
}
