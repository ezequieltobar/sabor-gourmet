package cl.ipss.saborgourmet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST del Restaurante "Sabor Gourmet".
 *
 * @SpringBootApplication activa la autoconfiguracion de Spring Boot y el escaneo de
 * componentes: Spring encuentra automaticamente las clases marcadas con
 * @RestController, @Service y @Repository dentro de este paquete y las conecta
 * entre si mediante inyeccion de dependencias.
 */
@SpringBootApplication
public class SaborGourmetApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaborGourmetApplication.class, args);
    }
}
