package cl.ipss.saborgourmet.model;

/**
 * Contrato comun para todo lo que se guarda en el archivo JSON con un identificador unico.
 *
 * Lo implementan Cliente, Mesa y Reserva. Gracias a esta interfaz el Repository puede
 * usar un mismo metodo generico para buscar, guardar y eliminar cualquiera de las tres
 * colecciones, en lugar de repetir el mismo codigo tres veces.
 */
public interface Identificable {

    Long getId();

    void setId(Long id);
}
