# Gestión de vehículos

Este proyecto sirve para gestionar los vehículos de una organización, sus conductores y las asignaciones de cada vehículo. Desde la aplicación se puede consultar el estado de la flota, registrar salidas y regresos, y administrar las cuentas que tienen acceso.

En este documento explico cómo está organizado el proyecto, qué hace cada parte y qué hay que preparar para ejecutarlo.

## Qué contiene el proyecto

- `src/main/java/com/ejemplo/vehiculos/`: código de la aplicación.
  - `controller/`: páginas web y rutas de la API.
  - `model/`: datos que utiliza la aplicación, como vehículos, conductores y asignaciones.
  - `repository/`: consultas y acceso a PostgreSQL.
  - `service/`: operaciones y reglas de funcionamiento.
  - `security/`: inicio de sesión, permisos y creación del primer administrador.
- `src/main/resources/templates/`: páginas de la aplicación, hechas con Thymeleaf.
- `src/main/resources/static/`: archivos estáticos, como los estilos CSS.
- `src/main/resources/application.properties`: configuración de Spring y conexión con PostgreSQL.
- `db/schema.sql`: tablas, relaciones y restricciones de la base de datos. Hay que ejecutarlo al preparar una base nueva.
- `src/test/`: pruebas del proyecto.
- `pom.xml`: dependencias y configuración de Maven.
- `mvnw` y `mvnw.cmd`: scripts para ejecutar Maven sin tener que instalarlo por separado.
- `Jenkinsfile`: proceso de pruebas, publicación de la imagen y despliegue en Docker Swarm.

## Qué se puede hacer

La aplicación tiene una interfaz web y una API REST para trabajar con los mismos datos.

- **Vehículos:** guardar matrícula, modelo, kilometraje y estado. Se pueden consultar y filtrar por estado.
- **Conductores:** guardar sus datos y departamento.
- **Asignaciones:** asignar un vehículo a un conductor para una ruta o para llevarlo al taller, y registrar su regreso.
- **Usuarios:** crear cuentas con rol de administrador o conductor. La administración de usuarios está reservada al administrador.

Al crear una asignación, el vehículo tiene que estar disponible. Si es una ruta, el conductor no puede tener otra ruta activa; las asignaciones de taller no tienen ese límite. El sistema guarda el kilometraje de salida y cambia el estado del vehículo a `EN_RUTA` o `TALLER`. Al finalizar la asignación, se registra el kilometraje de regreso y el vehículo vuelve a estar disponible.

Las asignaciones finalizadas se conservan como histórico. Por defecto, no se pueden borrar hasta que hayan pasado 365 días desde el regreso; este plazo se puede cambiar con `ASIGNACIONES_RETENCION_DIAS`.

## Acceso a la aplicación

Las páginas web usan un formulario de inicio de sesión. La API utiliza autenticación HTTP Basic.

Los administradores pueden gestionar vehículos, conductores y usuarios. Tanto los administradores como los conductores pueden consultar vehículos y conductores, y trabajar con asignaciones. Solo un administrador puede eliminar asignaciones del histórico.

Las páginas principales de la aplicación son:

- `/app`: panel principal.
- `/vehiculos`: consulta y gestión de vehículos.
- `/conductores`: consulta y gestión de conductores.
- `/asignaciones`: registro y seguimiento de asignaciones.
- `/usuarios`: gestión de cuentas, solo para administradores.

La API está disponible en estas rutas:

| Ruta | Para qué sirve |
|---|---|
| `/api/vehiculos` | Consultar vehículos y, como administrador, crearlos, modificarlos o eliminarlos. |
| `/api/conductores` | Consultar conductores y, como administrador, crearlos, modificarlos o eliminarlos. |
| `/api/asignaciones` | Consultar, crear y finalizar asignaciones. También permite consultar el plazo de retención. |
| `/api/usuarios` | Gestionar cuentas de usuario como administrador. |
| `/api/me` | Consultar el usuario autenticado y su rol. |

## Preparar el entorno

Para ejecutar el proyecto necesitas:

- Java 21.
- PostgreSQL en funcionamiento.
- Una base de datos y un usuario de PostgreSQL con permisos para crear y actualizar las tablas.

La configuración inicial de la conexión está en `src/main/resources/application.properties`. Por defecto, la aplicación busca PostgreSQL en `localhost:5432`, con la base `vehiculosdb` y el usuario `docker`. Puedes cambiar estos valores con las variables `DB_HOST`, `DB_PORT`, `DB_NAME` y `DB_USER`. La contraseña de la base de datos se lee de `DB_PASSWORD` o del secreto `postgres_password`.

### Crear la base de datos y las tablas

Primero crea la base de datos y asegúrate de que el usuario configurado puede acceder a ella. Después, desde la carpeta principal del proyecto, ejecuta el esquema:

```powershell
psql -U docker -d vehiculosdb -f .\db\schema.sql
```

El esquema define las cuatro tablas principales (`conductores`, `vehiculos`, `usuarios` y `asignaciones`), junto con sus relaciones y restricciones. Conviene ejecutarlo antes del primer arranque: Hibernate puede actualizar parte de la estructura, pero no sustituye las restricciones e índices definidos en este archivo.


## Cómo añadir una tabla nueva (ejemplo: sede) con relacion a vehiculos

Una sede tiene muchos vehículos y cada vehículo pertenece a una sola sede. Por eso se crea una tabla nueva, `sedes`, y se añade una columna en `vehiculos` que guarda el CIF de su sede. La sede no guarda ningún dato de sus vehículos.

Como la base de datos se crea desde cero, no hace falta ningún cambio puntual sobre una base existente: basta con que `schema.sql` describa todo.

### El camino completo

1. `Sede.java`, en `model`.
2. `schema.sql`, tabla `sedes` y columna nueva en `vehiculos`.
3. `Vehiculo.java`, la relación con `Sede`.
4. `SedeRepository` y una consulta nueva en `VehiculoRepository`.
5. `RecursoNoEncontradoException` y `SedeService`.
6. `VehiculoService`, para guardar la sede.
7. `SedeController` y permisos.
8. Compilar y probar en local.
9. Recrear la base y redesplegar con Jenkins.

### Paso 2: actualizar `schema.sql`

Hay dos cambios, y la regla es que lo que se escribe aquí debe coincidir con lo que se escribió en las clases Java (nombres de columna, longitudes y obligatoriedad).

- **Tabla `sedes`.** Debe escribirse **antes** que `vehiculos`, porque `vehiculos` va a apuntar a ella. Tiene cuatro columnas: `cif` (texto de 9 caracteres, es la clave de la tabla), `nombre` (obligatorio, 100 caracteres), `tlf` (opcional, 20) y `correo` (opcional, 100). Añade una restricción que compruebe el formato del CIF, igual que la validación de `Sede.java`.
- **Columna nueva en `vehiculos`.** Se llama `cif_sede`, es un texto de 9 caracteres y es **opcional**, para que un vehículo pueda existir sin sede. Se declara como clave foránea hacia `sedes`: la base de datos rechaza cualquier CIF que no exista en esa tabla. Añade también un índice sobre esa columna, como los que ya hay para las otras claves foráneas.

### Paso 3: añadir la relación en `Vehiculo.java`

Es el cambio que realmente conecta las dos tablas. Sin él, `sedes` existe pero los vehículos no saben a cuál pertenecen.

- Añade un campo `sede` que indique que **muchos vehículos pertenecen a una sede**, con la columna `cif_sede` como enlace. Copia el patrón de las relaciones de `Asignacion.java`.
- La relación es opcional: no le pongas ninguna validación de obligatoriedad.
- Añade su getter y su setter.
- No añadas una lista de vehículos dentro de `Sede`. La relación la lleva `Vehiculo`.

### Paso 4: `SedeRepository` y `VehiculoRepository`

- **`SedeRepository`.** Copia `ConductorRepository`, con una diferencia: el tipo de la clave es texto (`String`), no numérico (`Long`), porque la clave es el CIF. Los métodos básicos (buscar, guardar, borrar, comprobar si existe) ya vienen incluidos.
- **`VehiculoRepository`.** Añade la consulta `existsBySedeCif`, que dice si queda algún vehículo con una sede determinada. El nombre se forma a partir de las propiedades `sede` y `cif`. Se usará para impedir borrar sedes con vehículos.

### Paso 5: `RecursoNoEncontradoException` y `SedeService`

- **`RecursoNoEncontradoException`.** Su constructor solo acepta identificadores numéricos. Cambia el tipo del identificador para que admita cualquier valor, y así funcione con un CIF. El mensaje se queda igual.
- **`SedeService`.** Copia la estructura de `ConductorService`. Necesita `SedeRepository` y `VehiculoRepository`. Tiene cinco operaciones:
  - **Listar** todas las sedes (solo lectura).
  - **Obtener** una sede por su CIF. Si no existe, lanza `RecursoNoEncontradoException`.
  - **Crear.** Normaliza el CIF (quita espacios y lo pasa a mayúsculas) y rechaza si ya existe, con un conflicto.
  - **Actualizar** nombre, teléfono y correo. El CIF no se puede cambiar, porque los vehículos lo usan como enlace.
  - **Eliminar.** Si la sede todavía tiene vehículos, lo rechaza con un mensaje claro. Si no, la borra.

### Paso 6: guardar la sede en `VehiculoService`

`crear` y `actualizar` copian los campos del vehículo uno a uno. Si no añades la sede aquí, **se ignora en silencio** y nunca da error.

- Inyecta `SedeRepository` en el constructor.
- En ambos métodos, copia también la sede, usando una pequeña función auxiliar con estas reglas:
  - Si no llega sede, o llega con el CIF vacío, el vehículo se queda sin sede.
  - Si llega un CIF, se normaliza igual que en `SedeService` y se busca la sede. Si no existe, lanza `RecursoNoEncontradoException`.

### Paso 7: `SedeController` y permisos

- **`SedeController`.** Copia `ConductorController`, con la ruta `/api/sedes`. Tiene cinco rutas: listar, consultar una por CIF, crear (responde 201), modificar y eliminar (responde 204). El identificador de la ruta es el CIF, un texto, no un número. Las tres operaciones de escritura llevan `@PreAuthorize("hasRole('ADMIN')")`.
- **`SecurityConfig`.** En la cadena de la API, añade `/api/sedes/**` a las mismas reglas que `vehiculos` y `conductores`: lectura para ADMIN y CONDUCTOR, escritura solo para ADMIN. Así las reglas quedan declaradas en un único sitio y el `@PreAuthorize` actúa como segunda protección.

### Paso 8: compilar y probar en local

1. Ejecuta `.\mvnw.cmd test` y comprueba que compila y que los tests actuales siguen pasando.
2. Crea una base de datos local vacía y ejecuta `schema.sql` sobre ella.
3. Arranca la aplicación con `.\mvnw.cmd spring-boot:run`.
4. Prueba con la API, autenticándote como administrador:
   - Crear una sede.
   - Crear un vehículo indicando su sede, y comprobar que al consultarlo aparece.
   - Crear un vehículo con un CIF que no existe, y comprobar que da error 404.
   - Intentar crear una sede con un CIF repetido, y comprobar que da error 409.
   - Intentar borrar una sede que tiene vehículos, y comprobar que lo rechaza.
   - Borrar la sede cuando ya no tiene vehículos.

### Paso 9: recrear la base y redesplegar con Jenkins

El orden importa: **`schema.sql` debe ejecutarse antes de que arranque el servicio**. Si el servicio arranca con la base vacía, Hibernate crea las tablas por su cuenta sin las restricciones de `schema.sql`, y después ya no se pueden aplicar.

## Interfaz web de sedes

Esta parte se añade cuando la tabla `sedes`, su servicio y su API ya funcionan. La interfaz solo llama a `SedeService`, así que todas las reglas (CIF repetido, sede con vehículos, etc.) se reutilizan sin repetirlas.

### El camino completo

1. Rutas de sedes en `WebController`.
2. Plantillas `sedes/lista.html` y `sedes/formulario.html`.
3. Enlace "Sedes" en el menú.
4. Sede en el formulario y en la lista de vehículos.
5. Qué se ve y qué se permite según el rol.
6. Probar con los dos roles.

### Paso 1: rutas de sedes en `WebController`

Las rutas se añaden en `WebController`. Esa clase es la que aporta el dato `admin` a todas las páginas y la que tiene asociado el manejo de errores de `WebExceptionHandler`.

- Inyecta `SedeService` en el constructor.
- **Listar** (`GET /sedes`): carga todas las sedes y devuelve la plantilla de lista. Puede entrar cualquier usuario identificado.
- **Formulario de alta** (`GET /sedes/nueva`): envía una `Sede` vacía a la plantilla. Solo ADMIN.
- **Formulario de edición** (`GET /sedes/{cif}/editar`): carga la sede por su CIF. Solo ADMIN.
- **Crear** (`POST /sedes`): valida los datos. Si hay errores, vuelve al formulario; si no, llama a `SedeService.crear`, guarda el mensaje "Sede creada." y redirige a `/sedes`. Solo ADMIN.
- **Actualizar** (`POST /sedes/{cif}`): igual que crear, pero llama a `SedeService.actualizar` con el CIF de la ruta. Solo ADMIN.
- **Eliminar** (`POST /sedes/{cif}/eliminar`): llama a `SedeService.eliminar` y redirige a `/sedes`. Solo ADMIN.

Todas las rutas de escritura y los formularios llevan `@PreAuthorize("hasRole('ADMIN')")`, como las de vehículos y conductores.

Un detalle importante: en vehículos y conductores, el formulario sabe si está creando o editando mirando si el `id` es nulo. Con sedes **eso no sirve**, porque el CIF lo escribe el usuario. Si al crear se equivoca y el formulario vuelve con errores, el CIF ya tendría valor y el formulario creería que está editando. Por eso hay que enviar a la plantilla un dato aparte que diga si es edición (por ejemplo `edicion`, verdadero solo en la ruta de editar y al volver de un error al actualizar).

Cuando se elimina una sede que tiene vehículos, el servicio la rechaza y `WebExceptionHandler` muestra el mensaje en la página de inicio, igual que ocurre con los demás errores de la aplicación.

### Paso 2: plantillas de sedes

Crea la carpeta `templates/sedes/` y copia como base las de conductores.

- **`sedes/lista.html`** (basada en `conductores/lista.html`):
  - Columnas: CIF, nombre, teléfono y correo.
  - El teléfono y el correo, si están vacíos, muestran un guion.
  - El botón "Añadir sede" y la columna de acciones (Editar, Eliminar) solo se muestran si `admin` es verdadero.
  - Incluye los bloques de mensaje y de error, y la fila "No hay sedes registradas." cuando la lista esté vacía.
- **`sedes/formulario.html`** (basada en `conductores/formulario.html`):
  - Campos: CIF, nombre, teléfono y correo, cada uno con su texto de error debajo.
  - El título y la ruta a la que se envía el formulario dependen del dato `edicion`.
  - Cuando es edición, el CIF aparece **de solo lectura** (no desactivado: un campo desactivado no se envía), porque el CIF no se puede cambiar.

### Paso 3: enlace en el menú

En `fragments/nav.html` añade el enlace "Sedes" junto a Vehículos y Conductores. Es visible para todos los roles, igual que esos dos. Como el menú se incluye en todas las páginas, el cambio se refleja en toda la aplicación.

### Paso 4: sede en el formulario y en la lista de vehículos

- **`WebController`:** al preparar el formulario de vehículos, carga también la lista de sedes en el modelo.
- **`vehiculos/formulario.html`:** añade un selector "Sede".
  - La primera opción es "Sin sede" y tiene valor vacío.
  - Las demás muestran el nombre y el CIF de cada sede, y su valor es el CIF.
  - El selector se enlaza con el CIF de la sede del vehículo, de modo que en edición aparezca ya elegida la sede actual.
  - Si se elige "Sin sede", llega un CIF vacío. Por eso el servicio de vehículos debe tratar un CIF vacío como "sin sede" (Paso 6 de la sección anterior).
- **`vehiculos/lista.html`:** añade la columna "Sede" y muestra el nombre, o un guion si no tiene. Sube en uno el número de columnas de la fila "No hay vehículos registrados.".

Esta es la parte que más probablemente dé problemas. Si el formulario falla al abrirse para un vehículo sin sede con un error de "valor nulo en una propiedad anidada", hay dos soluciones: inicializar la sede con un objeto vacío al preparar el formulario, o enviar el CIF elegido como un parámetro aparte y pasárselo al servicio.

### Paso 5: qué se ve y qué se permite según el rol

Para cada acción reservada al administrador, se hacen **las dos cosas**:

- **En la pantalla**, se oculta con la condición de `admin` en la plantilla (el botón "Añadir sede", la columna de acciones, Editar y Eliminar).
- **En el servidor**, se protege con `@PreAuthorize("hasRole('ADMIN')")` en el método del controlador.

Ocultar un botón no protege nada: solo evita que el conductor lo vea. Lo que impide la acción es el permiso del servidor. En `SecurityConfig` no hace falta tocar nada para la parte web: las rutas `/sedes` siguen el mismo camino que `/vehiculos`, que exige estar identificado y delega el resto en `@PreAuthorize`.

### Paso 6: probar con los dos roles

1. Entra como administrador y comprueba que ves "Sedes" en el menú, el botón "Añadir sede" y las acciones de Editar y Eliminar.
2. Crea una sede. Comprueba que un CIF repetido o con mal formato da un error y no crea nada.
3. Edita un vehículo y asígnale la sede. Comprueba que aparece en la lista de vehículos y que, al abrir la edición, el selector muestra la sede actual.
4. Intenta eliminar la sede mientras tiene vehículos, y comprueba que se rechaza con un mensaje.
5. Entra como conductor. Debe ver "Sedes" en el menú y la lista, pero ni "Añadir sede" ni la columna de acciones.
6. Como conductor, escribe a mano `/sedes/nueva` en la barra de direcciones. Debe dar un error 403.



### Configurar el primer administrador

Al arrancar, la aplicación crea un usuario administrador únicamente si todavía no hay usuarios en la base de datos y se ha configurado una contraseña. No hay una contraseña predeterminada.

- `ADMIN_PASSWORD`: contraseña del administrador inicial.
- `ADMIN_USERNAME`: nombre de usuario inicial; por defecto es `admin`.

En despliegues con Docker también se puede proporcionar la contraseña mediante el secreto `admin_password`. La aplicación guarda la contraseña codificada y no la cambia si ya existen usuarios.

Para probarlo en PowerShell, configura las variables antes de iniciar la aplicación:

```powershell
$env:DB_PASSWORD = "contraseña de PostgreSQL"
$env:ADMIN_PASSWORD = "contraseña del administrador"
```

No guardes contraseñas reales en el repositorio.

## Ejecutar y probar

Desde PowerShell, en la carpeta principal del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

Cuando la aplicación esté en marcha, abre `http://localhost:8080`. Inicia sesión con el nombre y la contraseña de administrador configurados.

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

## Publicación y despliegue con Jenkins

El `Jenkinsfile` tiene dos opciones:

- **`testeo`**: ejecuta las pruebas y la verificación de Maven, prepara una imagen de la aplicación y la publica en Docker Hub con el número de build como etiqueta.
- **`despliegue`**: recibe en `TAG_IMAGEN` la etiqueta que se quiere desplegar en Docker Swarm.

El despliegue está preparado para el entorno definido en el pipeline. Necesita un Swarm activo, la red `RedPostgreSQLNginxTomcat`, el servicio PostgreSQL `docker-practicas_postgres` y los secretos Docker `postgres_password` y `admin_password`. Jenkins también debe tener configurada la credencial `docker_hub_token`.

Antes de crear el servicio, el pipeline comprueba que esos recursos existen. Después verifica que la aplicación responde; si el despliegue falla, intenta volver a utilizar la imagen anterior.
