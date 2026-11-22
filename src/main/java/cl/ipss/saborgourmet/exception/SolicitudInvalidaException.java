package cl.ipss.saborgourmet.exception;

/**
 * Se lanza cuando los datos enviados no cumplen una regla de negocio que no se puede
 * expresar con anotaciones. Por ejemplo: una fecha pasada o una hora fuera del horario
 * de atencion.
 * El ManejadorGlobalErrores la transforma en una respuesta 400 Bad Request.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
