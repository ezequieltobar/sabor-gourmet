package cl.ipss.saborgourmet.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.ipss.saborgourmet.exception.ConflictoException;
import cl.ipss.saborgourmet.exception.NoEncontradoException;
import cl.ipss.saborgourmet.exception.SolicitudInvalidaException;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.repository.DatosRepository;

/**
 * CAPA SERVICE de reservas: concentra las reglas de negocio de las reservas.
 *
 * CRITERIO DE DISPONIBILIDAD (definido por el grupo):
 * - Cada reserva ocupa la mesa durante 2 horas desde su hora de inicio.
 * - Se aceptan reservas entre las 12:00 y las 22:00 (la ultima termina a medianoche).
 * - Dos reservas ACTIVAS de la misma mesa y el mismo dia NO pueden superponerse.
 *   Ejemplo: una reserva a las 20:00 ocupa de 20:00 a 22:00, por lo que choca con
 *   una a las 21:00, pero no con una a las 22:00.
 * - Las reservas CANCELADAS no ocupan la mesa: cancelar libera la disponibilidad.
 */
@Service
public class ReservaService {

    /** Duracion de cada reserva, en minutos (2 horas). */
    public static final int DURACION_MINUTOS = 120;
    /** Primera hora en que se puede reservar. */
    public static final LocalTime HORA_APERTURA = LocalTime.of(12, 0);
    /** Ultima hora de inicio posible (termina a las 00:00, cuando cierra el local). */
    public static final LocalTime ULTIMA_HORA_RESERVA = LocalTime.of(22, 0);

    private final DatosRepository repository;

    public ReservaService(DatosRepository repository) {
        this.repository = repository;
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    /**
     * Lista reservas con filtros opcionales (si un filtro es null, no se aplica).
     * Permite al personal consultar, por ejemplo, las reservas ACTIVAS de un dia.
     * El resultado se ordena por fecha y hora.
     */
    public List<Reserva> listar(LocalDate fecha, EstadoReserva estado, Long clienteId) {
        return repository.findAllReservas().stream()
                .filter(r -> fecha == null || r.getFecha().equals(fecha))
                .filter(r -> estado == null || r.getEstado() == estado)
                .filter(r -> clienteId == null || r.getClienteId().equals(clienteId))
                .sorted(Comparator.comparing(Reserva::getFecha).thenComparing(Reserva::getHora))
                .toList();
    }

    /** Busca una reserva o lanza 404 si no existe. */
    public Reserva buscarPorId(Long id) {
        return repository.findReservaById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la reserva con id " + id));
    }

    // =====================================================================
    // Operaciones
    // =====================================================================

    /**
     * Crea una reserva nueva en estado ACTIVA.
     * "synchronized" evita que dos solicitudes al mismo tiempo reserven la misma mesa y horario.
     */
    public synchronized Reserva crear(Reserva reserva) {
        validarReserva(reserva, null);
        reserva.setId(null);
        reserva.setEstado(EstadoReserva.ACTIVA);
        return repository.saveReserva(reserva);
    }

    /**
     * Modifica una reserva existente (cliente, mesa, fecha, hora, personas u observaciones).
     * Solo se pueden modificar reservas ACTIVAS. Al validar la superposicion se excluye la
     * propia reserva, para que pueda cambiar, por ejemplo, solo la cantidad de personas.
     */
    public synchronized Reserva actualizar(Long id, Reserva datos) {
        Reserva actual = buscarPorId(id);
        if (actual.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("La reserva " + id + " esta cancelada y no se puede modificar");
        }
        validarReserva(datos, id);
        datos.setId(id);
        datos.setEstado(EstadoReserva.ACTIVA);
        return repository.saveReserva(datos);
    }

    /**
     * Cancela una reserva: cambia su estado a CANCELADA y lo guarda en el archivo.
     * No se elimina el registro (queda como historial), pero la mesa deja de estar ocupada
     * porque la disponibilidad solo considera reservas ACTIVAS.
     */
    public synchronized Reserva cancelar(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoException("La reserva " + id + " ya estaba cancelada");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        return repository.saveReserva(reserva);
    }

    // =====================================================================
    // Reglas de negocio
    // =====================================================================

    /**
     * Indica si una mesa ya tiene una reserva ACTIVA que se superpone con el horario pedido.
     * Tambien la usa MesaService para calcular las mesas disponibles.
     *
     * Dos intervalos [inicioA, finA) y [inicioB, finB) se superponen si:
     *     inicioA < finB  &&  inicioB < finA
     * Los horarios se pasan a minutos del dia (ej: 20:30 -> 1230) para que la reserva de
     * las 22:00 (que termina a las 24:00 = 1440) se compare correctamente.
     *
     * @param reservaExcluida id de una reserva que no se considera (la que se esta modificando), o null
     */
    public boolean mesaOcupada(Long mesaId, LocalDate fecha, LocalTime hora, Long reservaExcluida) {
        int inicio = hora.toSecondOfDay() / 60;
        int fin = inicio + DURACION_MINUTOS;
        return repository.findAllReservas().stream()
                .filter(r -> r.getEstado() == EstadoReserva.ACTIVA)
                .filter(r -> r.getMesaId().equals(mesaId))
                .filter(r -> r.getFecha().equals(fecha))
                .filter(r -> !r.getId().equals(reservaExcluida))
                .anyMatch(r -> {
                    int inicioOtra = r.getHora().toSecondOfDay() / 60;
                    int finOtra = inicioOtra + DURACION_MINUTOS;
                    return inicio < finOtra && inicioOtra < fin;
                });
    }

    /**
     * Valida fecha y hora: no en el pasado y dentro del horario de reservas.
     * Es publica porque MesaService la reutiliza al consultar disponibilidad.
     */
    public void validarFechaHora(LocalDate fecha, LocalTime hora) {
        if (hora.isBefore(HORA_APERTURA) || hora.isAfter(ULTIMA_HORA_RESERVA)) {
            throw new SolicitudInvalidaException("La hora debe estar entre " + HORA_APERTURA + " y "
                    + ULTIMA_HORA_RESERVA + " (cada reserva dura " + DURACION_MINUTOS / 60 + " horas)");
        }
        if (LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now())) {
            throw new SolicitudInvalidaException("No se puede reservar en una fecha u hora que ya paso");
        }
    }

    /**
     * Todas las validaciones de una reserva, en orden:
     * 1. Fecha y hora validas (400).
     * 2. El cliente existe (404).
     * 3. La mesa existe (404).
     * 4. La cantidad de personas no supera la capacidad de la mesa (409).
     * 5. La mesa no tiene otra reserva activa superpuesta (409).
     * (Los campos obligatorios y cantidades positivas ya los valido @Valid en el Controller.)
     */
    private void validarReserva(Reserva reserva, Long reservaExcluida) {
        validarFechaHora(reserva.getFecha(), reserva.getHora());

        repository.findClienteById(reserva.getClienteId())
                .orElseThrow(() -> new NoEncontradoException("No existe el cliente con id " + reserva.getClienteId()));

        Mesa mesa = repository.findMesaById(reserva.getMesaId())
                .orElseThrow(() -> new NoEncontradoException("No existe la mesa con id " + reserva.getMesaId()));

        if (reserva.getCantidadPersonas() > mesa.getCapacidad()) {
            throw new ConflictoException("La mesa " + mesa.getNumero() + " tiene capacidad para "
                    + mesa.getCapacidad() + " personas y se solicitaron " + reserva.getCantidadPersonas());
        }

        if (mesaOcupada(mesa.getId(), reserva.getFecha(), reserva.getHora(), reservaExcluida)) {
            throw new ConflictoException("La mesa " + mesa.getNumero() + " ya tiene una reserva activa que se "
                    + "superpone con el " + reserva.getFecha() + " a las " + reserva.getHora()
                    + " (cada reserva dura " + DURACION_MINUTOS / 60 + " horas)");
        }
    }
}
