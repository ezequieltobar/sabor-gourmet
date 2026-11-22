package cl.ipss.saborgourmet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import cl.ipss.saborgourmet.repository.DatosRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Prueba de integracion de la API completa.
 *
 * Levanta la aplicacion con un archivo JSON propio de pruebas (target/test-data/datos-test.json),
 * para no tocar data/datos.json, y simula solicitudes HTTP con MockMvc, verificando el codigo
 * HTTP y el JSON de cada respuesta.
 */
@SpringBootTest(properties = "app.archivo-json=" + SaborGourmetApplicationTests.ARCHIVO_PRUEBA)
@AutoConfigureMockMvc
class SaborGourmetApplicationTests {

    static final String ARCHIVO_PRUEBA = "target/test-data/datos-test.json";

    // Se borra el archivo de pruebas antes de levantar la aplicacion, para partir siempre desde cero.
    static {
        try {
            Files.deleteIfExists(Path.of(ARCHIVO_PRUEBA));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String FECHA = LocalDate.now().plusDays(10).toString();

    @Test
    void flujoCompletoDeReservas() throws Exception {
        // 1. Crear cliente y mesa -> 201
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Perez\",\"email\":\"ana@correo.cl\",\"telefono\":\"+56911112222\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mvc.perform(post("/api/mesas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":10,\"capacidad\":4,\"ubicacion\":\"Terraza\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        // 2. Crear reserva -> 201 con estado ACTIVA
        String reserva = "{\"clienteId\":1,\"mesaId\":1,\"fecha\":\"" + FECHA
                + "\",\"hora\":\"20:00\",\"cantidadPersonas\":3}";
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(reserva))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        // 3. Reserva superpuesta (21:00 choca con 20:00-22:00) -> 409
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                .content(reserva.replace("20:00", "21:00")))
                .andExpect(status().isConflict());

        // 4. Supera la capacidad de la mesa -> 409
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                .content(reserva.replace("20:00", "13:00").replace("\"cantidadPersonas\":3", "\"cantidadPersonas\":9")))
                .andExpect(status().isConflict());

        // 5. La mesa no aparece disponible a las 21:00
        mvc.perform(get("/api/mesas/disponibles").param("fecha", FECHA).param("hora", "21:00").param("personas", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 6. No se puede eliminar un cliente con reservas activas -> 409
        mvc.perform(delete("/api/clientes/1")).andExpect(status().isConflict());

        // 7. Modificar la reserva (cambia la cantidad de personas) -> 200
        mvc.perform(put("/api/reservas/1").contentType(MediaType.APPLICATION_JSON)
                .content(reserva.replace("\"cantidadPersonas\":3", "\"cantidadPersonas\":4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadPersonas").value(4));

        // 8. Cancelar -> 200 con estado CANCELADA; cancelar de nuevo -> 409
        mvc.perform(patch("/api/reservas/1/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(patch("/api/reservas/1/cancelar")).andExpect(status().isConflict());

        // 9. La cancelacion libero la mesa: ahora si se puede reservar a las 21:00 -> 201
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                .content(reserva.replace("20:00", "21:00")))
                .andExpect(status().isCreated());

        // 10. Errores: datos invalidos -> 400, inexistente -> 404, metodo no permitido -> 405
        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"email\":\"no-es-email\",\"telefono\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.email").exists());
        mvc.perform(get("/api/reservas/999")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/reservas/1")).andExpect(status().isMethodNotAllowed());

        // 11. Persistencia: un Repository nuevo lee el archivo y encuentra los mismos datos
        //     (equivale a reiniciar la aplicacion).
        DatosRepository recargado = new DatosRepository(objectMapper, ARCHIVO_PRUEBA);
        assertThat(recargado.findAllClientes()).hasSize(1);
        assertThat(recargado.findAllMesas()).hasSize(1);
        assertThat(recargado.findAllReservas()).hasSize(2);
        assertThat(recargado.findReservaById(1L).orElseThrow().getEstado().name()).isEqualTo("CANCELADA");
    }
}
