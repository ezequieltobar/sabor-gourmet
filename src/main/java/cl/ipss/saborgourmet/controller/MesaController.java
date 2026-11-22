package cl.ipss.saborgourmet.controller;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.service.MesaService;
import jakarta.validation.Valid;

/**
 * CAPA CONTROLLER de mesas: configuracion del salon y consulta de disponibilidad.
 *
 * Endpoints:
 *   GET    /api/mesas                                                -> 200 lista de mesas
 *   GET    /api/mesas/disponibles?fecha=2026-12-24&hora=20:00&personas=4 -> 200 mesas libres | 400
 *   GET    /api/mesas/{id}                                           -> 200 mesa | 404
 *   POST   /api/mesas                                                -> 201 mesa creada | 400 | 409
 *   PUT    /api/mesas/{id}                                           -> 200 mesa actualizada | 400 | 404 | 409
 *   DELETE /api/mesas/{id}                                           -> 204 | 404 | 409
 */
@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    public ResponseEntity<List<Mesa>> listar() {
        return ResponseEntity.ok(mesaService.listar());
    }

    /**
     * Mesas disponibles para una fecha, hora y cantidad de personas.
     * Los tres parametros son obligatorios; si falta uno o tiene mal formato se responde 400.
     * Spring prioriza la ruta fija "/disponibles" por sobre "/{id}", por eso no se confunden.
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Mesa>> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime hora,
            @RequestParam int personas) {
        return ResponseEntity.ok(mesaService.buscarDisponibles(fecha, hora, personas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mesa> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mesaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Mesa> crear(@Valid @RequestBody Mesa mesa) {
        Mesa creada = mesaService.crear(mesa);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mesa> actualizar(@PathVariable Long id, @Valid @RequestBody Mesa mesa) {
        return ResponseEntity.ok(mesaService.actualizar(id, mesa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mesaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
