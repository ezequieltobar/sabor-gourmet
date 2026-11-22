package cl.ipss.saborgourmet.model;

/**
 * Estados posibles de una reserva.
 *
 * Se usa un enum (y no un texto libre) para que solo existan estos valores validos.
 * Solo las reservas ACTIVA ocupan una mesa; al cancelar, la reserva pasa a CANCELADA
 * y la mesa queda libre automaticamente, pero el registro se conserva como historial.
 */
public enum EstadoReserva {
    ACTIVA,
    CANCELADA
}
