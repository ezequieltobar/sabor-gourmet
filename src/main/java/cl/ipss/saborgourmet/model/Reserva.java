package cl.ipss.saborgourmet.model;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Reserva de una mesa por parte de un cliente.
 *
 * La reserva NO guarda el objeto Cliente ni el objeto Mesa completos, sino solo sus
 * identificadores (clienteId y mesaId). Asi se mantienen las referencias entre registros
 * sin duplicar informacion en el archivo JSON: si cambia el telefono del cliente, el cambio
 * se hace en un solo lugar.
 *
 * Cada reserva ocupa la mesa durante 2 horas desde su hora de inicio
 * (ver ReservaService.DURACION_MINUTOS).
 */
public class Reserva implements Identificable {

    /** Identificador unico. Lo asigna el sistema. */
    private Long id;

    /** Referencia al cliente que reserva (id de la coleccion "clientes"). */
    @NotNull(message = "El clienteId es obligatorio")
    private Long clienteId;

    /** Referencia a la mesa reservada (id de la coleccion "mesas"). */
    @NotNull(message = "El mesaId es obligatorio")
    private Long mesaId;

    /** Dia de la reserva. Formato JSON: "yyyy-MM-dd" (por ejemplo "2026-12-24"). */
    @NotNull(message = "La fecha es obligatoria (formato yyyy-MM-dd)")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    /** Hora de inicio. Formato JSON: "HH:mm" (por ejemplo "20:30"). */
    @NotNull(message = "La hora es obligatoria (formato HH:mm)")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime hora;

    @NotNull(message = "La cantidad de personas es obligatoria")
    @Positive(message = "La cantidad de personas debe ser mayor que 0")
    private Integer cantidadPersonas;

    /** Estado actual. Lo controla el sistema: se crea ACTIVA y pasa a CANCELADA al cancelar. */
    private EstadoReserva estado;

    /** Comentario opcional del cliente (alergias, celebracion, etc.). */
    @Size(max = 200, message = "Las observaciones no pueden superar los 200 caracteres")
    private String observaciones;

    public Reserva() {
    }

    public Reserva(Long id, Long clienteId, Long mesaId, LocalDate fecha, LocalTime hora,
            Integer cantidadPersonas, EstadoReserva estado, String observaciones) {
        this.id = id;
        this.clienteId = clienteId;
        this.mesaId = mesaId;
        this.fecha = fecha;
        this.hora = hora;
        this.cantidadPersonas = cantidadPersonas;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public void setMesaId(Long mesaId) {
        this.mesaId = mesaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public Integer getCantidadPersonas() {
        return cantidadPersonas;
    }

    public void setCantidadPersonas(Integer cantidadPersonas) {
        this.cantidadPersonas = cantidadPersonas;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
