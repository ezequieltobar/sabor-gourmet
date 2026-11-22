package cl.ipss.saborgourmet.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.service.ClienteService;
import jakarta.validation.Valid;

/**
 * CAPA CONTROLLER de clientes.
 *
 * Responsabilidad: recibir la solicitud HTTP, pasarle los datos al Service y devolver un
 * ResponseEntity con el codigo HTTP correcto. No contiene reglas de negocio ni accede al archivo.
 *
 * Endpoints:
 *   GET    /api/clientes        -> 200 lista de clientes
 *   GET    /api/clientes/{id}   -> 200 cliente | 404
 *   POST   /api/clientes        -> 201 cliente creado | 400 | 409 (email repetido)
 *   PUT    /api/clientes/{id}   -> 200 cliente actualizado | 400 | 404 | 409
 *   DELETE /api/clientes/{id}   -> 204 sin cuerpo | 404 | 409 (tiene reservas activas)
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    /**
     * @Valid activa las validaciones del modelo (nombre, email, telefono).
     * Responde 201 Created con el cliente creado y la cabecera Location con su URL.
     */
    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody Cliente cliente) {
        Cliente creado = clienteService.crear(cliente);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteService.actualizar(id, cliente));
    }

    /** 204 No Content: la eliminacion fue exitosa y no hay cuerpo que devolver. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
