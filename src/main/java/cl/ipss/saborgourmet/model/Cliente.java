package cl.ipss.saborgourmet.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cliente del restaurante: la persona que realiza las reservas.
 *
 * Las anotaciones de validacion se revisan automaticamente cuando el Controller
 * recibe el cuerpo de la solicitud con @Valid. Si alguna falla, la API responde
 * 400 Bad Request indicando el campo y el motivo.
 */
public class Cliente implements Identificable {

    /** Identificador unico. Lo asigna el sistema al crear el cliente. */
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    /** Correo de contacto. No puede repetirse entre clientes. */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    private String email;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(min = 8, max = 15, message = "El telefono debe tener entre 8 y 15 caracteres")
    private String telefono;

    /** Constructor vacio requerido por Jackson para crear el objeto al leer el JSON. */
    public Cliente() {
    }

    public Cliente(Long id, String nombre, String email, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
