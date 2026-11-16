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

## Acceso y seguridad

La aplicación tiene dos formas de acceso: un inicio de sesión con formulario para las páginas web y autenticación HTTP Basic para la API. Aunque ambos accesos consultan los mismos usuarios de la tabla `usuarios`, cada uno mantiene su propia sesión o forma de enviar las credenciales.

### Cómo inicia sesión un usuario en la página web

1. El usuario se guarda en la tabla `usuarios` con un nombre, una contraseña y un rol. La contraseña no se guarda en texto normal: `UsuarioService` la codifica con BCrypt antes de guardarla.
2. Al abrir `/login`, se muestra el formulario de `src/main/resources/templates/login.html`. El navegador envía el nombre y la contraseña a Spring Security.
3. `UsuarioDetailsService` busca el nombre en la tabla `usuarios` y carga su contraseña codificada y su rol. Spring Security compara la contraseña introducida con el hash guardado.
4. Si las credenciales son correctas, se inicia una sesión web. El navegador conserva la cookie de sesión `JSESSIONID` y la envía al navegar por la aplicación. Al cerrar sesión, Spring Security invalida esa sesión.

El primer administrador se crea al arrancar solo si la tabla `usuarios` está vacía y se ha configurado `ADMIN_PASSWORD` o el secreto `admin_password`. El nombre se puede definir con `ADMIN_USERNAME`; si no se hace, será `admin`. A partir de ahí, el administrador puede crear usuarios desde `/usuarios` y asignarles el rol `ADMIN` o `CONDUCTOR`.

### Qué permisos tiene cada rol

| Acción | Administrador | Conductor |
|---|---:|---:|
| Entrar en las páginas privadas y consultar vehículos y conductores | Sí | Sí |
| Crear, modificar o eliminar vehículos y conductores | Sí | No |
| Consultar, crear y finalizar asignaciones | Sí | Sí |
| Eliminar asignaciones antiguas o depurar el histórico | Sí | No |
| Consultar, crear, modificar o eliminar usuarios | Sí | No |

El control no depende solo de lo que se muestra en el menú: los permisos se comprueban en el servidor. `SecurityConfig` protege las rutas y los métodos de `WebController` y los controllers de la API aplican comprobaciones adicionales con `@PreAuthorize` cuando hace falta.

Para las páginas web, las rutas de `/usuarios/**` son exclusivas de ADMIN y las demás páginas privadas requieren que el usuario haya iniciado sesión. Los formularios que modifican datos mantienen la protección CSRF de Spring Security.

La API `/api/**` usa HTTP Basic y no mantiene una sesión entre peticiones: el cliente envía sus credenciales en cada llamada. Las consultas de vehículos y conductores y las operaciones de asignaciones están disponibles para ambos roles; los cambios en vehículos y conductores, la gestión de usuarios y el borrado del histórico se limitan a ADMIN. La API responde `401` si faltan credenciales o no son válidas y `403` si el usuario no tiene permisos. En un entorno real, la API debe utilizarse mediante HTTPS para proteger las credenciales durante el envío.

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

## Añadir una tabla nueva

Para que una tabla nueva forme parte de la aplicación no basta con crearla en PostgreSQL. También hay que decidir cómo se va a consultar o modificar desde los servicios, la API y, si corresponde, las páginas Thymeleaf.

Por ejemplo, si se quisiera guardar mantenimientos de vehículos:

1. **Definir qué datos y relaciones necesita.** Decidir qué campos tendrá la tabla y si se relaciona con un vehículo, un conductor u otra tabla.
2. **Crear la entidad** en `src/main/java/com/ejemplo/vehiculos/model/`, por ejemplo `Mantenimiento.java`. Se marca con `@Entity` y `@Table`, y se definen sus campos, tipos, relaciones y validaciones. Para una relación hay que decidir también qué debe ocurrir si se elimina el registro relacionado.
3. **Añadir la tabla a `db/schema.sql`.** Incluir columnas, clave primaria, claves foráneas, restricciones e índices necesarios. Este archivo es el que deja definidas las reglas SQL que Hibernate no crea por sí solo con la misma precisión.
4. **Crear el acceso a datos** en `repository/`, normalmente con una interfaz que extienda `JpaRepository`. Aquí se añaden las consultas que necesite el caso de uso.
5. **Implementar las operaciones y reglas** en una clase de `service/`. Por ejemplo, comprobar que el vehículo existe o que una fecha de mantenimiento es válida. Las reglas de negocio deben estar en el servicio para que se apliquen tanto desde la web como desde la API.
6. **Añadir la API si se necesita.** Crear rutas en un controller de `controller/` y definir los datos que acepta y devuelve. Revisar también los permisos de esa ruta en `SecurityConfig` y usar `@PreAuthorize` cuando corresponda.
7. **Añadir las páginas si se necesita gestionarla desde la web.** Crear o modificar rutas en `WebController`, pasar a la plantilla los datos que necesite y preparar las vistas en `src/main/resources/templates/`. Si la nueva sección debe aparecer en la navegación, actualizar `templates/fragments/nav.html`. Los formularios deben enviar los campos y mostrar sus errores de validación.
8. **Añadir pruebas** para las reglas nuevas y comprobar tanto los permisos como las operaciones desde los puntos de acceso que se hayan creado.

Al añadir una entidad, `spring.jpa.hibernate.ddl-auto=update` puede crear la tabla al arrancar. Aun así, para que se creen también las restricciones e índices previstos, hay que definirlos en `db/schema.sql` y ejecutar ese esquema al preparar la base.

### Qué hacer para empezar con una base de datos nueva

Si se elimina la base de datos y se quiere volver a empezar desde cero:

1. Hacer una copia de seguridad si se quiere conservar algún dato. Borrar la base elimina usuarios, vehículos, conductores y el histórico de asignaciones.
2. Añadir la entidad y los cambios de SQL siguiendo los pasos anteriores.
3. Crear de nuevo la base de datos y ejecutar el `db/schema.sql` actualizado antes de iniciar la aplicación.
4. Desplegar o arrancar la versión nueva de la aplicación. Al iniciar, Hibernate revisará las entidades y `DataInitializer` creará el primer administrador si se cumple la configuración descrita en la sección de acceso.

En este proyecto, el pipeline de Jenkins no ejecuta `db/schema.sql`: publica o despliega la aplicación, pero no crea la base ni aplica ese archivo. Por eso, al recrear la base en Docker Swarm hay que preparar el esquema en PostgreSQL por separado antes de arrancar el servicio de la aplicación. Eliminar únicamente el servicio `gestion-vehiculos` tampoco borra la base de datos ni sus datos; son servicios distintos.

## Modificar una tabla existente

Si se empieza de cero junto con la base de datos, el cambio debe quedar reflejado en el modelo Java y en la definición SQL para que la nueva instalación nazca con la estructura correcta. También hay que revisar todos los lugares de la aplicación que leen o escriben esos datos:

1. **Cambiar la entidad** correspondiente en `model/`. Actualizar el campo, su tipo, las anotaciones JPA, las validaciones y, si aplica, la relación con otras entidades.
2. **Actualizar `db/schema.sql`.** Cambiar la definición de la tabla y revisar claves foráneas, valores permitidos, restricciones e índices relacionados. Si se ha cambiado un enum o sus etiquetas, actualizar también el enum, su conversor y las restricciones SQL.
3. **Revisar el servicio y el repositorio.** Cambiar las reglas o consultas que dependan del campo. Si cambia el nombre de una propiedad, revisar los métodos de consulta de Spring Data que se basan en ese nombre.
4. **Revisar la API.** Actualizar los DTO, las validaciones y las respuestas de los controllers. Si cambia el formato que devuelve un endpoint, los clientes que lo consumen también tendrán que adaptarse.
5. **Revisar Thymeleaf.** Actualizar los formularios y las listas en `src/main/resources/templates/`, además de los datos que `WebController` envía a cada vista. Si cambia la navegación, revisar `templates/fragments/nav.html`.
6. **Actualizar las pruebas** y verificar que la operación funciona desde la API y desde la página web, con los permisos adecuados.
7. **Crear la base de nuevo con el esquema actualizado** y desplegar la aplicación que contiene los cambios.

Si la base contiene datos que se deben conservar, no se debe borrar para aplicar el cambio. `ddl-auto=update` puede añadir algunas columnas o tablas, pero no es un sistema de migraciones completo: no hay que confiar en él para renombrar o eliminar columnas ni para transformar datos. En ese caso hace falta preparar y ejecutar una migración SQL compatible con los datos existentes antes de desplegar el código nuevo.

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

## Otros temas que se podrían documentar

- Cómo hacer y restaurar copias de seguridad de PostgreSQL.
- Cómo preparar migraciones SQL cuando se necesita conservar los datos existentes.
- Cómo probar los permisos de cada rol y las respuestas de la API.
- Cómo consultar los logs y diagnosticar errores durante el despliegue.
