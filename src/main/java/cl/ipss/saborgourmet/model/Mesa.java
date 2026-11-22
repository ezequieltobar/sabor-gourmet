package cl.ipss.saborgourmet.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Mesa del restaurante. El personal puede crearlas, modificarlas y eliminarlas
 * para mantener actualizada la configuracion del salon.
 */
public class Mesa implements Identificable {

    /** Identificador unico interno. Lo asigna el sistema. */
    private Long id;

    /** Numero visible de la mesa en el salon. No puede repetirse. */
    @NotNull(message = "El numero de mesa es obligatorio")
    @Positive(message = "El numero de mesa debe ser mayor que 0")
    private Integer numero;

    /** Cantidad maxima de personas que caben en la mesa. */
    @NotNull(message = "La capacidad es obligatoria")
    @Positive(message = "La capacidad debe ser mayor que 0")
    private Integer capacidad;

    /** Ubicacion opcional dentro del local (por ejemplo: Terraza, Salon, Ventana). */
    @Size(max = 50, message = "La ubicacion no puede superar los 50 caracteres")
    private String ubicacion;

    public Mesa() {
    }

    public Mesa(Long id, Integer numero, Integer capacidad, String ubicacion) {
        this.id = id;
        this.numero = numero;
        this.capacidad = capacidad;
        this.ubicacion = ubicacion;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}
