# Sabor Gourmet – API REST de reservas

Evaluación Unidad 2 – Desarrollo de Software Web II.
API REST con Spring Boot 4, Java 21, arquitectura **Controller-Service-Repository (CSR)** y persistencia en un **archivo JSON**.

## Requisitos
- Java 21 (JDK)
- Maven, o el Maven Wrapper (`mvnw`). Para usar el wrapper, copia `mvnw`, `mvnw.cmd` y la carpeta oculta `.mvn` desde el proyecto `smartphone` a la raíz de este proyecto.
- Postman, para probar la API

## Cómo ejecutar
Desde la raíz del proyecto (la carpeta que contiene `pom.xml`):

```bash
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
# Con Maven instalado
mvn spring-boot:run
```

También se puede ejecutar `SaborGourmetApplication.java` con el botón *Run* de VS Code o IntelliJ. En ese caso, el directorio de trabajo debe ser la raíz del proyecto.

La API queda disponible en **http://localhost:8080**.

Pruebas automáticas: `mvnw test` (o `mvn test`). Usan un archivo aparte (`target/test-data/datos-test.json`), así que no modifican los datos reales.

## Archivo JSON
- **Ruta:** `data/datos.json`. Se configura en `application.properties` con `app.archivo-json=data/datos.json`.
- Si el archivo no existe, la aplicación lo crea al iniciar con `{"clientes":[],"mesas":[],"reservas":[]}`.
- Cada creación, modificación, eliminación o cancelación se guarda de inmediato, por lo que los datos se conservan al reiniciar.
- **Estructura:**

```json
{
  "clientes": [ { "id": 1, "nombre": "Camila Rojas", "email": "camila.rojas@correo.cl", "telefono": "+56912345678" } ],
  "mesas":    [ { "id": 3, "numero": 3, "capacidad": 4, "ubicacion": "Salon" } ],
  "reservas": [ { "id": 1, "clienteId": 1, "mesaId": 3, "fecha": "2026-12-24", "hora": "20:00",
                  "cantidadPersonas": 4, "estado": "ACTIVA", "observaciones": "Cena de Nochebuena" } ]
}
```

## Reglas de disponibilidad
- Cada reserva ocupa la mesa **2 horas** desde su hora de inicio.
- Se puede reservar entre las **12:00 y las 22:00**, siempre en una fecha y hora futuras.
- Una mesa no puede tener dos reservas **ACTIVAS** que se superpongan. Por ejemplo, una reserva a las 20:00 choca con una a las 21:00, pero no con una a las 22:00.
- Al **cancelar**, la reserva pasa a `CANCELADA` y la mesa queda libre. El registro se conserva como historial.

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/clientes` | Lista los clientes | 200 |
| GET | `/api/clientes/{id}` | Obtiene un cliente | 200, 404 |
| POST | `/api/clientes` | Crea un cliente | 201, 400, 409 |
| PUT | `/api/clientes/{id}` | Actualiza un cliente | 200, 400, 404, 409 |
| DELETE | `/api/clientes/{id}` | Elimina un cliente | 204, 404, 409 |
| GET | `/api/mesas` | Lista las mesas | 200 |
| GET | `/api/mesas/{id}` | Obtiene una mesa | 200, 404 |
| GET | `/api/mesas/disponibles?fecha=2026-12-24&hora=20:30&personas=4` | Mesas disponibles | 200, 400 |
| POST | `/api/mesas` | Crea una mesa | 201, 400, 409 |
| PUT | `/api/mesas/{id}` | Actualiza una mesa | 200, 400, 404, 409 |
| DELETE | `/api/mesas/{id}` | Elimina una mesa | 204, 404, 409 |
| GET | `/api/reservas?fecha=&estado=&clienteId=` | Lista reservas (filtros opcionales) | 200, 400 |
| GET | `/api/reservas/{id}` | Obtiene una reserva | 200, 404 |
| POST | `/api/reservas` | Crea una reserva | 201, 400, 404, 409 |
| PUT | `/api/reservas/{id}` | Modifica una reserva activa | 200, 400, 404, 409 |
| **PATCH** | **`/api/reservas/{id}/cancelar`** | **Cancela una reserva** | 200, 404, 409 |

Códigos de error: **400** datos inválidos · **404** no existe · **409** conflicto (superposición, capacidad, duplicado o relación activa) · **405** método no permitido. Los errores devuelven JSON y nunca muestran trazas internas.

## Postman
Importa `postman/SaborGourmet.postman_collection.json` en Postman. Contiene 38 solicitudes en tres carpetas (Clientes, Mesas y Reservas), con casos exitosos y casos de error. Cada solicitud verifica automáticamente el código HTTP esperado. Para ejecutarla completa, usa *Run collection* con la aplicación corriendo y el `datos.json` original.

## Estructura
```
src/main/java/cl/ipss/saborgourmet
├── SaborGourmetApplication.java
├── model/        Cliente, Mesa, Reserva, EstadoReserva, BaseDatos, Identificable
├── repository/   DatosRepository     (lee y escribe datos.json)
├── service/      ClienteService, MesaService, ReservaService (reglas de negocio)
├── controller/   ClienteController, MesaController, ReservaController (HTTP + ResponseEntity)
└── exception/    excepciones propias + ManejadorGlobalErrores (400/404/409)
```
