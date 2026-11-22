# Sabor Gourmet – API REST de reservas

Evaluación Unidad 2 – Desarrollo de Software Web II.
API REST con Spring Boot 4, Java 21, arquitectura **Controller-Service-Repository (CSR)** y persistencia en un **archivo JSON**.

## Requisitos
- Java 21 (JDK), con la variable `JAVA_HOME` apuntando a ella
- Maven no es necesario: el proyecto incluye el Maven Wrapper (`mvnw`, `mvnw.cmd` y la carpeta `.mvn`)
- Postman o Thunder Client, para probar la API

> ¿Primera vez con el proyecto? Lee antes la sección **[Guía para el equipo](#guía-para-el-equipo)**, al final de este archivo.

## Cómo ejecutar
Desde la raíz del proyecto (la carpeta que contiene `pom.xml`):

```bash
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
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

---

## Guía para el equipo

> Esta sección es solo para el trabajo del grupo. Se puede borrar antes de la entrega final.

### 1. Preparar el computador (una sola vez)

**a) Tener la JDK 21.** En una terminal ejecuta:
```
java -version
```
No importa si muestra otra versión (24, 25, etc.); lo importante es que exista una carpeta `jdk-21...` dentro de `C:\Program Files\Java\`. Si no está, instala **Eclipse Temurin 21 (LTS)** desde [adoptium.net](https://adoptium.net) y, en el instalador, activa **Add to PATH** y **Set JAVA_HOME variable**.

> Si VS Code ofrece instalar una JDK más nueva (por ejemplo, la 27), **no es necesario**: el proyecto usa Java 21.

**b) Configurar `JAVA_HOME` de forma permanente.** `mvnw.cmd` no funciona sin ella.
1. En el buscador de Windows escribe **"Editar las variables de entorno del sistema"** y haz clic en **Variables de entorno**.
2. En **Variables del sistema**, haz clic en **Nueva** (o en **Editar**, si ya existe `JAVA_HOME`).
   - Nombre: `JAVA_HOME`
   - Valor: la carpeta de tu JDK 21, por ejemplo `C:\Program Files\Java\jdk-21.0.12.1`. Copia la ruta exacta desde el Explorador.
3. Acepta todo y **cierra VS Code y todas las terminales**.
4. Al volver a abrir, comprueba en la terminal de VS Code (PowerShell):
   ```
   echo $env:JAVA_HOME
   ```
   Debe mostrar la ruta de la JDK 21.

> **Ojo:** en PowerShell (la terminal cuya línea empieza con `PS`), `echo %JAVA_HOME%` **no funciona** y siempre muestra el texto `%JAVA_HOME%`. Usa `echo $env:JAVA_HOME`.

**Atajo temporal** (solo dura mientras la terminal esté abierta):
```
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
```

**c) VS Code** con las extensiones **Extension Pack for Java** y **Spring Boot Extension Pack**.

### 2. Descargar el proyecto
```
git clone https://github.com/ezequieltobar/sabor-gourmet.git
cd sabor-gourmet
```
Abre la carpeta `sabor-gourmet` en VS Code con **Archivo → Abrir carpeta**.

### 3. Comprobar que todo funciona
```
.\mvnw.cmd test
```
La primera vez demora unos minutos, porque descarga Maven y las librerías. Debe terminar en **BUILD SUCCESS** con `Tests run: 1, Failures: 0, Errors: 0`.
Los mensajes `WARNING` sobre Mockito o los "Java agent" **no son errores** y se pueden ignorar.

### 4. Levantar la API y probarla
1. Ejecuta `.\mvnw.cmd spring-boot:run` y espera el mensaje `Started SaborGourmetApplication`.
2. En Postman o Thunder Client, importa `postman/SaborGourmet.postman_collection.json`.
3. Ejecuta la colección completa (**Run collection**) **sin cambiar el orden** de las solicitudes. Deben pasar las **38**. Las que se llaman "ERROR 400/404/409/405" también deben salir en verde, porque verifican que la API responda con ese error.
4. Para detener la API: `Ctrl + C` en la terminal.

### 5. Cómo trabajar con Git sin pisarnos
**Antes de empezar a trabajar**, trae los cambios de los demás:
```
git pull
```
**Al terminar**, sube tus cambios:
```
git status
git add .
git commit -m "Descripción corta de lo que hiciste"
git push
```
Reglas:
- **No subas `data/datos.json` después de probar la API**, porque las pruebas lo modifican. Si `git status` lo muestra como modificado, déjalo como estaba con:
  ```
  git restore data/datos.json
  ```
- Haz commits pequeños y con mensajes claros, por ejemplo: `Comentarios en ReservaService`.
- Si `git push` es rechazado, haz primero `git pull` y luego vuelve a hacer `git push`.
- La carpeta `target/` nunca se sube (ya está en `.gitignore`).

### 6. Reparto de tareas

| | Integrante 1 – Base y persistencia | Integrante 2 – Clientes y mesas | Integrante 3 – Reservas y disponibilidad |
|---|---|---|---|
| **Archivos** | `pom.xml`, `application.properties`, `SaborGourmetApplication`, `model/*`, `DatosRepository`, `data/datos.json` | `ClienteService`, `ClienteController`, `MesaService`, `MesaController`, `exception/*` | `ReservaService`, `ReservaController` |
| **Debe saber explicar** | CSR, modelo de datos, por qué hay un solo Repository, la escritura segura con `.tmp`, cómo se crea el JSON si no existe | Validaciones, respuestas 200/201/204, el bloqueo 409 al eliminar con reservas activas, el manejador global de errores | Las reglas de 2 horas y de 12:00 a 22:00, la superposición en minutos, la cancelación con PATCH y la consulta de disponibilidad |
| **Pruebas** | Persistencia y reinicio de la aplicación | Carpetas 1 y 2 de Postman | Carpeta 3 de Postman |
| **Informe** | Portada, introducción, CSR, modelos, Repository, JSON | Services y Controllers de clientes y mesas, sus endpoints, índice, bibliografía | Service y Controller de reservas, sus endpoints, instrucciones de ejecución, conclusión |

> `MesaService` usa `reservaService.mesaOcupada(...)` para calcular la disponibilidad. Los integrantes 2 y 3 deben revisar juntos esa parte.

### 7. Problemas frecuentes

| Problema | Solución |
|---|---|
| `JAVA_HOME environment variable is not defined correctly` | Configura `JAVA_HOME` (paso 1b) y reinicia VS Code. |
| `echo %JAVA_HOME%` muestra `%JAVA_HOME%` | Estás en PowerShell: usa `echo $env:JAVA_HOME`. |
| `Port 8080 was already in use` | La API ya está corriendo en otra terminal. Detenla con `Ctrl + C` o cierra esa terminal. |
| La colección de Postman falla en varias solicitudes | `datos.json` quedó modificado por pruebas anteriores. Ejecuta `git restore data/datos.json`, reinicia la API y vuelve a correr la colección. |
| `.\mvnw.cmd` no se reconoce | La terminal no está en la carpeta del proyecto. Usa `cd` hasta la carpeta que contiene `pom.xml`. |
