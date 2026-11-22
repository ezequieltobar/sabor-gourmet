package cl.ipss.saborgourmet.exception;

/**
 * Se lanza cuando se busca un cliente, mesa o reserva que no existe.
 * El ManejadorGlobalErrores la transforma en una respuesta 404 Not Found.
 */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
