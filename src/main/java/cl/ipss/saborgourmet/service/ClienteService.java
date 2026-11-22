package cl.ipss.saborgourmet.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.ipss.saborgourmet.exception.ConflictoException;
import cl.ipss.saborgourmet.exception.NoEncontradoException;
import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.model.EstadoReserva;
import cl.ipss.saborgourmet.repository.DatosRepository;

/**
 * CAPA SERVICE de clientes: reglas de negocio del CRUD de clientes.
 *
 * Reglas:
 * - El email no puede repetirse entre clientes (409).
 * - No se puede eliminar un cliente con reservas activas desde hoy en adelante (409),
 *   para no dejar reservas apuntando a un cliente que no existe.
 */
@Service
public class ClienteService {

    private final DatosRepository repository;

    /** Inyeccion de dependencias por constructor: Spring entrega el Repository automaticamente. */
    public ClienteService(DatosRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listar() {
        return repository.findAllClientes();
    }

    /** Busca un cliente o lanza 404 si no existe. */
    public Cliente buscarPorId(Long id) {
        return repository.findClienteById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el cliente con id " + id));
    }

    public Cliente crear(Cliente cliente) {
        validarEmailUnico(cliente.getEmail(), null);
        cliente.setId(null); // el id siempre lo asigna el sistema
        cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        return repository.saveCliente(cliente);
    }

    public Cliente actualizar(Long id, Cliente datos) {
        buscarPorId(id); // 404 si no existe
        validarEmailUnico(datos.getEmail(), id);
        datos.setId(id);
        datos.setEmail(datos.getEmail().trim().toLowerCase());
        return repository.saveCliente(datos);
    }

    public void eliminar(Long id) {
        buscarPorId(id);
        boolean tieneReservasActivas = repository.findAllReservas().stream()
                .anyMatch(r -> r.getClienteId().equals(id)
                        && r.getEstado() == EstadoReserva.ACTIVA
                        && !r.getFecha().isBefore(LocalDate.now()));
        if (tieneReservasActivas) {
            throw new ConflictoException("No se puede eliminar el cliente " + id
                    + " porque tiene reservas activas. Cancele sus reservas primero.");
        }
        repository.deleteClienteById(id);
    }

    /** Verifica que ningun OTRO cliente use el mismo email (sin distinguir mayusculas). */
    private void validarEmailUnico(String email, Long idActual) {
        boolean repetido = repository.findAllClientes().stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(email.trim()) && !c.getId().equals(idActual));
        if (repetido) {
            throw new ConflictoException("Ya existe un cliente registrado con el email " + email);
        }
    }
}
