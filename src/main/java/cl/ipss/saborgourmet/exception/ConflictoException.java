package cl.ipss.saborgourmet.exception;

/**
 * Se lanza cuando la solicitud es correcta en su forma, pero choca con el estado actual
 * de los datos. Por ejemplo: la mesa ya esta reservada en ese horario, la mesa no tiene
 * capacidad suficiente, el email ya esta registrado o se intenta eliminar un cliente que
 * tiene reservas activas.
 * El ManejadorGlobalErrores la transforma en una respuesta 409 Conflict.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
