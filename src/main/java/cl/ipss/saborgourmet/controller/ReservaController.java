package cl.ipss.saborgourmet.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.model.Reserva;
import cl.ipss.saborgourmet.service.ReservaService;
import jakarta.validation.Valid;

/**
 * CAPA CONTROLLER de reservas.
 *
 * Endpoints:
 *   GET   /api/reservas                         -> 200 lista (filtros opcionales: fecha, estado, clienteId)
 *   GET   /api/reservas/{id}                    -> 200 reserva | 404
 *   POST  /api/reservas                         -> 201 reserva creada | 400 | 404 | 409
 *   PUT   /api/reservas/{id}                    -> 200 reserva modificada | 400 | 404 | 409
 *   PATCH /api/reservas/{id}/cancelar           -> 200 reserva con estado CANCELADA | 404 | 409
 *
 * CANCELACION: se usa PATCH porque se modifica solo una parte de la reserva (su estado).
 * La reserva no se borra; queda como historial y la mesa se libera. Por eso no existe
 * DELETE para reservas (si se intenta, la API responde 405 Method Not Allowed).
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * Ejemplos:
     *   GET /api/reservas                                  (todas)
     *   GET /api/reservas?estado=ACTIVA                    (reservas actuales)
     *   GET /api/reservas?fecha=2026-12-24&estado=ACTIVA   (reservas activas de un dia)
     *   GET /api/reservas?clienteId=1                      (historial de un cliente)
     */
    @GetMapping
    public ResponseEntity<List<Reserva>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(reservaService.listar(fecha, estado, clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Reserva> crear(@Valid @RequestBody Reserva reserva) {
        Reserva creada = reservaService.crear(reserva);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reserva> actualizar(@PathVariable Long id, @Valid @RequestBody Reserva reserva) {
        return ResponseEntity.ok(reservaService.actualizar(id, reserva));
    }

    /** Cancela la reserva y devuelve 200 con la reserva actualizada (estado CANCELADA). */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Reserva> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelar(id));
    }
}
