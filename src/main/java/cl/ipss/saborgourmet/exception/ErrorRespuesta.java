package cl.ipss.saborgourmet.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cuerpo JSON que devuelve la API cuando ocurre un error.
 *
 * Todas las respuestas de error tienen la misma forma, por ejemplo:
 * {
 *   "fecha": "2026-11-20T15:30:00",
 *   "status": 409,
 *   "error": "Conflict",
 *   "mensaje": "La mesa 3 ya tiene una reserva activa que se superpone ...",
 *   "ruta": "/api/reservas"
 * }
 *
 * Se usa un "record" de Java: una clase inmutable que solo transporta datos,
 * y Java genera automaticamente su constructor y sus metodos de acceso.
 * El campo "detalles" solo aparece cuando hay errores por campo (validaciones).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fecha,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> detalles) {
}
