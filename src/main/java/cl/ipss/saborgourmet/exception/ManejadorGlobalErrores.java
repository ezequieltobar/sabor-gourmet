package cl.ipss.saborgourmet.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejador GLOBAL de errores de la API.
 *
 * @RestControllerAdvice hace que esta clase "escuche" las excepciones lanzadas por
 * cualquier Controller. Cada metodo @ExceptionHandler indica que excepcion atiende y
 * con que codigo HTTP responde. Asi:
 *  - los Controllers y Services no necesitan bloques try/catch repetidos;
 *  - todas las respuestas de error tienen el mismo formato JSON (ErrorRespuesta);
 *  - nunca se muestran trazas internas (stack traces) al usuario.
 *
 * Tabla de codigos:
 *   400 Bad Request  -> datos invalidos (validaciones, JSON mal formado, parametros incorrectos)
 *   404 Not Found    -> el recurso (cliente, mesa, reserva o ruta) no existe
 *   405 Not Allowed  -> metodo HTTP no permitido en esa ruta
 *   409 Conflict     -> la operacion choca con los datos actuales (superposicion, capacidad, etc.)
 *   500 Server Error -> error inesperado o problema con el archivo JSON
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    /** 404: el Service no encontro el cliente, la mesa o la reserva solicitada. */
    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(NoEncontradoException ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /** 409: la operacion entra en conflicto con los datos existentes. */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(ConflictoException ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: regla de negocio no cumplida (fecha pasada, hora fuera de horario, etc.). */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> solicitudInvalida(SolicitudInvalidaException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /**
     * 400: fallo alguna anotacion de validacion (@NotBlank, @Positive, @Email...) del cuerpo
     * recibido con @Valid. Se devuelve un mapa "campo -> motivo" en "detalles".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "Hay campos con datos invalidos", req, detalles);
    }

    /** 400: el cuerpo no es un JSON valido o trae un valor con formato incorrecto (fecha, hora, numero). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es un JSON valido o tiene un formato incorrecto "
                        + "(fecha: yyyy-MM-dd, hora: HH:mm, numeros sin comillas)",
                req, null);
    }

    /** 400: un parametro de la URL tiene un tipo incorrecto (ej: /api/mesas/abc o ?fecha=hoy). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoIncorrecto(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parametro '" + ex.getName() + "' tiene un valor invalido: " + ex.getValue(), req, null);
    }

    /** 400: falta un parametro obligatorio en la URL (ej: ?fecha= en /api/mesas/disponibles). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> parametroFaltante(MissingServletRequestParameterException ex,
            HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST,
                "Falta el parametro obligatorio '" + ex.getParameterName() + "'", req, null);
    }

    /** 404: la ruta solicitada no existe en la API. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", req, null);
    }

    /** 405: se uso un metodo HTTP que la ruta no admite (ej: DELETE /api/reservas/1). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodoNoPermitido(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest req) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED,
                "El metodo " + ex.getMethod() + " no esta permitido en esta ruta", req, null);
    }

    /** 500: no se pudo leer o escribir datos.json. El detalle tecnico queda solo en el log del servidor. */
    @ExceptionHandler(PersistenciaException.class)
    public ResponseEntity<ErrorRespuesta> persistencia(PersistenciaException ex, HttpServletRequest req) {
        log.error("Error de persistencia en {}", req.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo guardar o leer la informacion. Los datos existentes no fueron modificados.", req, null);
    }

    /** 500: cualquier otro error inesperado. Se registra en el log, pero al usuario no se le muestra la traza. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno en el servidor", req, null);
    }

    /** Arma el ResponseEntity con el codigo HTTP y el cuerpo ErrorRespuesta. */
    private ResponseEntity<ErrorRespuesta> construir(HttpStatus status, String mensaje, HttpServletRequest req,
            Map<String, String> detalles) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), mensaje, req.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
