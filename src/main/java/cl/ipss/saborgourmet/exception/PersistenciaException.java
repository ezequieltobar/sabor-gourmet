package cl.ipss.saborgourmet.exception;

/**
 * Se lanza cuando no se puede leer o escribir el archivo datos.json
 * (archivo daniado, sin permisos, disco lleno, etc.).
 * El ManejadorGlobalErrores la transforma en una respuesta 500 sin mostrar detalles internos.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
