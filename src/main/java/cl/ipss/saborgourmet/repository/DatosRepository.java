package cl.ipss.saborgourmet.repository;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import cl.ipss.saborgourmet.exception.PersistenciaException;
import cl.ipss.saborgourmet.model.BaseDatos;
import cl.ipss.saborgourmet.model.Cliente;
import cl.ipss.saborgourmet.model.Identificable;
import cl.ipss.saborgourmet.model.Mesa;
import cl.ipss.saborgourmet.model.Reserva;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * CAPA REPOSITORY: unica clase que lee y escribe el archivo JSON.
 *
 * Responsabilidad: persistencia. No conoce reglas de negocio (eso es del Service);
 * solo sabe buscar, guardar y eliminar registros, y mantener el archivo actualizado.
 *
 * Como funciona:
 * 1. Al iniciar la aplicacion lee datos.json y lo carga en memoria (objeto BaseDatos).
 *    Si el archivo no existe, lo crea con una estructura valida y vacia.
 * 2. Las consultas se responden desde memoria (rapido, sin leer el disco cada vez).
 * 3. CADA cambio (crear, modificar, eliminar o cancelar) se escribe inmediatamente en
 *    el archivo, por eso los datos se conservan al reiniciar la aplicacion.
 *
 * Se usa un solo Repository para las tres colecciones porque viven en un mismo archivo:
 * si hubiera tres repositories escribiendo el mismo archivo, uno podria pisar los
 * cambios del otro.
 *
 * Los metodos publicos son "synchronized" para que dos solicitudes simultaneas no
 * modifiquen los datos al mismo tiempo.
 */
@Repository
public class DatosRepository {

    private static final Logger log = LoggerFactory.getLogger(DatosRepository.class);

    /** Componente de Jackson que convierte objetos Java <-> JSON. Lo entrega Spring Boot. */
    private final ObjectMapper objectMapper;

    /** Ruta del archivo JSON, configurada en application.properties (app.archivo-json). */
    private final Path archivo;

    /** Copia en memoria del contenido del archivo. */
    private BaseDatos datos;

    public DatosRepository(ObjectMapper objectMapper, @Value("${app.archivo-json}") String ruta) {
        this.objectMapper = objectMapper;
        this.archivo = Path.of(ruta).toAbsolutePath();
        inicializar();
    }

    // =====================================================================
    // Lectura y escritura del archivo
    // =====================================================================

    /**
     * Prepara el archivo al arrancar:
     * - si no existe (o esta vacio), lo crea con {"clientes":[],"mesas":[],"reservas":[]};
     * - si existe, lo lee.
     * Si el archivo existe pero esta daniado, NO se sobrescribe: la aplicacion se detiene con un
     * mensaje claro para que se pueda revisar, sin perder la informacion.
     */
    private void inicializar() {
        try {
            if (archivo.getParent() != null) {
                Files.createDirectories(archivo.getParent());
            }
            if (Files.notExists(archivo) || Files.size(archivo) == 0) {
                datos = new BaseDatos();
                escribirArchivo();
                log.info("Archivo de datos creado con estructura vacia en {}", archivo);
            } else {
                datos = leerArchivo();
                log.info("Datos cargados desde {}: {} clientes, {} mesas, {} reservas", archivo,
                        datos.getClientes().size(), datos.getMesas().size(), datos.getReservas().size());
            }
        } catch (IOException | JacksonException e) {
            throw new PersistenciaException("No se pudo inicializar el archivo de datos " + archivo
                    + ". Revise que el JSON sea valido; el archivo no fue modificado.", e);
        }
    }

    /** Deserializacion: convierte el contenido del archivo JSON en un objeto BaseDatos. */
    private BaseDatos leerArchivo() {
        return objectMapper.readValue(archivo.toFile(), BaseDatos.class);
    }

    /**
     * Serializacion: convierte BaseDatos a JSON y lo guarda en el archivo.
     *
     * Escritura segura: primero se escribe un archivo temporal (datos.json.tmp) y luego
     * se reemplaza el original de una sola vez. Si algo falla a mitad de la escritura,
     * el archivo original queda intacto y nunca queda un JSON a medio escribir.
     */
    private void escribirArchivo() throws IOException {
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), datos);
        try {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Guarda en el archivo los cambios hechos en memoria.
     * Si la escritura falla, se vuelve a cargar el archivo para descartar el cambio en memoria
     * (asi memoria y archivo siguen iguales) y se informa el error.
     */
    private void guardarCambios() {
        try {
            escribirArchivo();
        } catch (IOException | JacksonException e) {
            try {
                datos = leerArchivo();
            } catch (JacksonException ignorada) {
                // Si tampoco se puede releer, se conserva lo que hay en memoria.
            }
            throw new PersistenciaException("No se pudo escribir el archivo de datos " + archivo, e);
        }
    }

    // =====================================================================
    // Metodos genericos (sirven para clientes, mesas y reservas)
    // =====================================================================

    /** Busca un registro por id dentro de una coleccion. */
    private <T extends Identificable> Optional<T> buscarPorId(List<T> lista, Long id) {
        return lista.stream().filter(e -> e.getId().equals(id)).findFirst();
    }

    /**
     * Guarda un registro:
     * - si no tiene id, es NUEVO: se le asigna el siguiente id (mayor id + 1) y se agrega;
     * - si tiene id, es una ACTUALIZACION: se reemplaza el registro con ese id.
     * Luego se escribe el archivo.
     */
    private <T extends Identificable> T guardar(List<T> lista, T entidad) {
        if (entidad.getId() == null) {
            long siguienteId = lista.stream().mapToLong(Identificable::getId).max().orElse(0) + 1;
            entidad.setId(siguienteId);
            lista.add(entidad);
        } else {
            lista.removeIf(e -> e.getId().equals(entidad.getId()));
            lista.add(entidad);
            lista.sort((a, b) -> a.getId().compareTo(b.getId()));
        }
        guardarCambios();
        return entidad;
    }

    /** Elimina un registro por id y escribe el archivo. */
    private <T extends Identificable> void eliminar(List<T> lista, Long id) {
        if (lista.removeIf(e -> e.getId().equals(id))) {
            guardarCambios();
        }
    }

    // =====================================================================
    // Clientes
    // =====================================================================

    public synchronized List<Cliente> findAllClientes() {
        return new ArrayList<>(datos.getClientes());
    }

    public synchronized Optional<Cliente> findClienteById(Long id) {
        return buscarPorId(datos.getClientes(), id);
    }

    public synchronized Cliente saveCliente(Cliente cliente) {
        return guardar(datos.getClientes(), cliente);
    }

    public synchronized void deleteClienteById(Long id) {
        eliminar(datos.getClientes(), id);
    }

    // =====================================================================
    // Mesas
    // =====================================================================

    public synchronized List<Mesa> findAllMesas() {
        return new ArrayList<>(datos.getMesas());
    }

    public synchronized Optional<Mesa> findMesaById(Long id) {
        return buscarPorId(datos.getMesas(), id);
    }

    public synchronized Mesa saveMesa(Mesa mesa) {
        return guardar(datos.getMesas(), mesa);
    }

    public synchronized void deleteMesaById(Long id) {
        eliminar(datos.getMesas(), id);
    }

    // =====================================================================
    // Reservas (no se eliminan: al cancelar cambian de estado y se guardan como historial)
    // =====================================================================

    public synchronized List<Reserva> findAllReservas() {
        return new ArrayList<>(datos.getReservas());
    }

    public synchronized Optional<Reserva> findReservaById(Long id) {
        return buscarPorId(datos.getReservas(), id);
    }

    public synchronized Reserva saveReserva(Reserva reserva) {
        return guardar(datos.getReservas(), reserva);
    }
}
