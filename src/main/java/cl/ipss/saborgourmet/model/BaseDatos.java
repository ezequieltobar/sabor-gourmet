package cl.ipss.saborgourmet.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa el contenido COMPLETO del archivo datos.json.
 *
 * Jackson convierte esta clase a JSON (serializacion) y desde JSON (deserializacion)
 * con la siguiente estructura:
 *
 * {
 *   "clientes": [ {...}, {...} ],
 *   "mesas":    [ {...}, {...} ],
 *   "reservas": [ {...}, {...} ]
 * }
 *
 * Los setters reemplazan un valor null por una lista vacia, para que un archivo
 * incompleto (por ejemplo "{}") igual se cargue con una estructura valida.
 */
public class BaseDatos {

    private List<Cliente> clientes = new ArrayList<>();
    private List<Mesa> mesas = new ArrayList<>();
    private List<Reserva> reservas = new ArrayList<>();

    public List<Cliente> getClientes() {
        return clientes;
    }

    public void setClientes(List<Cliente> clientes) {
        this.clientes = clientes != null ? new ArrayList<>(clientes) : new ArrayList<>();
    }

    public List<Mesa> getMesas() {
        return mesas;
    }

    public void setMesas(List<Mesa> mesas) {
        this.mesas = mesas != null ? new ArrayList<>(mesas) : new ArrayList<>();
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas != null ? new ArrayList<>(reservas) : new ArrayList<>();
    }
}
