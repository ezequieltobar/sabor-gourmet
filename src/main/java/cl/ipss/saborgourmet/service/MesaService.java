package cl.ipss.saborgourmet.service;

import java.time.LocalDate;
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
 * CAPA SERVICE de mesas: CRUD de mesas (configuracion del salon) y consulta de disponibilidad.
 *
 * Reglas:
 * - El numero de mesa no puede repetirse (409).
 * - No se puede bajar la capacidad por debajo de una reserva activa ya hecha (409).
 * - No se puede eliminar una mesa con reservas activas desde hoy en adelante (409).
 */
@Service
public class MesaService {

    private final DatosRepository repository;
    /** Se reutiliza la regla de superposicion de ReservaService para no duplicar logica. */
    private final ReservaService reservaService;

    public MesaService(DatosRepository repository, ReservaService reservaService) {
        this.repository = repository;
        this.reservaService = reservaService;
    }

    public List<Mesa> listar() {
        return repository.findAllMesas();
    }

    /** Busca una mesa o lanza 404 si no existe. */
    public Mesa buscarPorId(Long id) {
        return repository.findMesaById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la mesa con id " + id));
    }

    public Mesa crear(Mesa mesa) {
        validarNumeroUnico(mesa.getNumero(), null);
        mesa.setId(null);
        return repository.saveMesa(mesa);
    }

    public Mesa actualizar(Long id, Mesa datos) {
        buscarPorId(id);
        validarNumeroUnico(datos.getNumero(), id);

        // Si se reduce la capacidad, no puede quedar menor que una reserva activa existente.
        reservasActivasFuturas(id).stream()
                .filter(r -> r.getCantidadPersonas() > datos.getCapacidad())
                .findFirst()
                .ifPresent(r -> {
                    throw new ConflictoException("No se puede reducir la capacidad a " + datos.getCapacidad()
                            + ": la reserva " + r.getId() + " es para " + r.getCantidadPersonas() + " personas");
                });

        datos.setId(id);
        return repository.saveMesa(datos);
    }

    public void eliminar(Long id) {
        buscarPorId(id);
        if (!reservasActivasFuturas(id).isEmpty()) {
            throw new ConflictoException("No se puede eliminar la mesa " + id
                    + " porque tiene reservas activas. Cancele o mueva esas reservas primero.");
        }
        repository.deleteMesaById(id);
    }

    /**
     * Consulta de disponibilidad: devuelve las mesas con capacidad suficiente que NO tienen
     * una reserva activa superpuesta en la fecha y hora indicadas.
     * Se ordenan de menor a mayor capacidad para sugerir primero la mesa mas ajustada al grupo.
     */
    public List<Mesa> buscarDisponibles(LocalDate fecha, LocalTime hora, int personas) {
        if (personas <= 0) {
            throw new SolicitudInvalidaException("La cantidad de personas debe ser mayor que 0");
        }
        reservaService.validarFechaHora(fecha, hora);

        return repository.findAllMesas().stream()
                .filter(m -> m.getCapacidad() >= personas)
                .filter(m -> !reservaService.mesaOcupada(m.getId(), fecha, hora, null))
                .sorted(Comparator.comparing(Mesa::getCapacidad).thenComparing(Mesa::getNumero))
                .toList();
    }

    /** Reservas ACTIVAS de una mesa desde hoy en adelante (las pasadas ya no la bloquean). */
    private List<Reserva> reservasActivasFuturas(Long mesaId) {
        return repository.findAllReservas().stream()
                .filter(r -> r.getMesaId().equals(mesaId))
                .filter(r -> r.getEstado() == EstadoReserva.ACTIVA)
                .filter(r -> !r.getFecha().isBefore(LocalDate.now()))
                .toList();
    }

    /** Verifica que ninguna OTRA mesa tenga el mismo numero. */
    private void validarNumeroUnico(Integer numero, Long idActual) {
        boolean repetido = repository.findAllMesas().stream()
                .anyMatch(m -> m.getNumero().equals(numero) && !m.getId().equals(idActual));
        if (repetido) {
            throw new ConflictoException("Ya existe una mesa con el numero " + numero);
        }
    }
}
