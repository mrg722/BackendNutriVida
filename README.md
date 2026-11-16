# Backend Clínica NutriVida · Evaluación Parcial 3

Backend del caso **Forma D - Clínica Nutricional NutriVida** para DSY1104 Desarrollo FullStack II.
Está dividido en dos microservicios Spring Boot independientes, cada uno con su propio puerto,
su propia base de datos MySQL y su propio proceso. El frontend React vive en un repositorio
aparte: [ClinicaNutriVidaEP2](https://github.com/mrg722/ClinicaNutriVidaEP2).

## Arquitectura

| Proyecto | Responsabilidad | Puerto | Base de datos |
|---|---|---|---|
| `nutrivida-auth` | Usuarios, roles, inicio de sesión y emisión del JWT | 8081 | `nutrivida_auth` |
| `nutrivida-citas` | Catálogo de servicios, agenda de citas, fichas clínicas y estadísticas | 8082 | `nutrivida_citas` |

Los dos proyectos no comparten clases ni tablas. La única relación entre ellos es el token:
`nutrivida-auth` lo firma y `nutrivida-citas` verifica localmente la firma y el emisor, sin
consultar la base de usuarios en cada petición. Para que eso funcione, ambos reciben el mismo
valor en la variable `JWT_SECRET`.

```
React (5173) ──login──> nutrivida-auth (8081) ──firma el JWT──┐
     │                                                         │
     └──Bearer <token>──> nutrivida-citas (8082) ──verifica la firma y el rol
```

## Roles y permisos

Son los tres roles que exige el caso. El control real está en `SecurityConfig` de cada
microservicio y, cuando depende de la pertenencia del dato, en la capa de servicio.

| Rol | Puede |
|---|---|
| `ADMIN` | Todo: gestión de usuarios y roles, catálogo, directorio, todas las citas y las estadísticas |
| `NUTRICIONISTA` | Ver su propia agenda, mover el estado de esas citas y escribir en las fichas que él registra |
| `PACIENTE` | Ver el catálogo, pedir, reprogramar y cancelar sus propias citas, y leer su historial y su plan |

La regla de privacidad del caso está implementada: un paciente solo accede a sus datos y un
nutricionista no ve las mediciones registradas por otro profesional. Los intentos sin token
responden `401` y los intentos con token válido pero sin permiso responden `403`.

## Requisitos

- JDK 21.
- MySQL 8 en el puerto 3306.
- No hace falta instalar Maven: cada proyecto trae su propio `mvnw`.

## Puesta en marcha local

1. Creo las dos bases de datos. Puedo usar el script `despliegue/01-crear-bases-y-usuarios.sql`,
   que además crea una cuenta de MySQL por microservicio:

```bash
mysql -u root -p < despliegue/01-crear-bases-y-usuarios.sql
```

2. Levanto el servicio de autenticación:

```bash
cd nutrivida-auth && ./mvnw spring-boot:run
```

3. En otra terminal levanto el servicio de citas:

```bash
cd nutrivida-citas && ./mvnw spring-boot:run
```

En Windows uso `.\mvnw.cmd` en lugar de `./mvnw`. Las tablas las crea JPA al arrancar
(`spring.jpa.hibernate.ddl-auto=update`) y el catálogo del caso se carga solo la primera vez,
desde la clase `DatosIniciales` de cada proyecto.

Si quiero usar las cuentas de MySQL del script en vez de `root`, paso las credenciales por
variables de entorno antes de arrancar, sin tocar el código:

```bash
AUTH_DB_USER=nutrivida_auth_app AUTH_DB_PASSWORD=mi_clave ./mvnw spring-boot:run
```

## Documentación de la API

Con cada servicio arriba, Swagger UI queda en:

- http://localhost:8081/swagger-ui/index.html
- http://localhost:8082/swagger-ui/index.html

Para probar un endpoint protegido ejecuto `POST /api/v1/auth/login` en el Swagger de auth, copio
el valor de `token` y lo pego en el botón **Authorize** del Swagger de citas.

El detalle de cada endpoint, con su método, cuerpo, respuestas y roles, está en
[`documentacion/DOCUMENTACION_APIS.md`](documentacion/DOCUMENTACION_APIS.md).

## Endpoints

Todas las rutas van versionadas con el prefijo `/api/v1`.

### nutrivida-auth

| Método y ruta | Acceso |
|---|---|
| `POST /api/v1/auth/login` | Público |
| `POST /api/v1/auth/registro` | Público, crea una cuenta de paciente |
| `GET /api/v1/auth/perfil` | Cualquier sesión válida |
| `GET /api/v1/usuarios` | `ADMIN` |
| `GET /api/v1/usuarios/{id}` | `ADMIN` |
| `POST /api/v1/usuarios` | `ADMIN` |
| `PUT /api/v1/usuarios/{id}` | `ADMIN` |
| `DELETE /api/v1/usuarios/{id}` | `ADMIN` |

### nutrivida-citas

| Método y ruta | Acceso |
|---|---|
| `GET /api/v1/servicios` | Cualquier sesión válida |
| `POST`, `PUT`, `DELETE /api/v1/servicios` | `ADMIN` |
| `GET /api/v1/nutricionistas` | Cualquier sesión válida |
| `POST`, `PUT`, `DELETE /api/v1/nutricionistas` | `ADMIN` |
| `GET /api/v1/citas/mias` | Cualquier sesión válida, devuelve según el rol |
| `GET /api/v1/citas` | `ADMIN` |
| `POST /api/v1/citas` | `PACIENTE` o `ADMIN` |
| `PUT /api/v1/citas/{id}` | Dueño de la cita, su nutricionista o `ADMIN` |
| `PUT /api/v1/citas/{id}/estado` | Igual, con el paciente limitado a cancelar |
| `DELETE /api/v1/citas/{id}` | `ADMIN` |
| `GET /api/v1/mediciones/mias` | `PACIENTE` de la sesión |
| `GET /api/v1/mediciones/paciente/{correo}` | Dueño, su nutricionista o `ADMIN` |
| `POST`, `PUT`, `DELETE /api/v1/mediciones` | `NUTRICIONISTA` o `ADMIN` |
| `GET /api/v1/estadisticas` | `ADMIN` |

## Modelo de datos

`nutrivida_auth` tiene una sola tabla, `usuarios`, con el correo único y la contraseña
guardada como hash BCrypt.

`nutrivida_citas` tiene cuatro tablas, normalizadas a tercera forma normal:

- `servicios`: el catálogo del caso, con el código como identificador de negocio.
- `nutricionistas`: el directorio de los cuatro profesionales.
- `citas`: llaves foráneas a `servicios` y `nutricionistas`, más el correo del paciente. El
  precio y la duración no se repiten aquí: viven en `servicios`.
- `mediciones`: la ficha clínica. No guardo el IMC porque es un dato derivado del peso y la
  talla; lo calculo al momento de responder.

El paciente no tiene tabla en esta base: su identidad es el correo que viaja en el token, porque
sus datos personales son responsabilidad de `nutrivida-auth`.

## Cuentas iniciales

Se crean al arrancar `nutrivida-auth`, todas con la contraseña `Cambiar123!`. Están solo para
probar el sistema y hay que cambiarlas antes de cualquier despliegue real.

| Correo | Rol |
|---|---|
| `admin@nutrivida.cl` | `ADMIN` |
| `carolina.fuentes@nutrivida.cl` | `NUTRICIONISTA` |
| `rodrigo.sepulveda@nutrivida.cl` | `NUTRICIONISTA` |
| `daniela.morales@nutrivida.cl` | `NUTRICIONISTA` |
| `felipe.araya@nutrivida.cl` | `NUTRICIONISTA` |
| `paciente@nutrivida.cl` | `PACIENTE` |

La contraseña de estas cuentas se puede cambiar sin recompilar, con la variable
`APP_DATOS_INICIALES_PASSWORD`.

## Pruebas

Cada proyecto tiene una prueba de contexto que comprueba que la aplicación arranca, que la
conexión a MySQL funciona y que todos los beans quedaron bien declarados:

```bash
cd nutrivida-auth && ./mvnw test
cd nutrivida-citas && ./mvnw test
```

Necesitan MySQL arriba, porque levantan el contexto completo contra la base real.

## Despliegue

La carpeta `despliegue/` reúne lo necesario para publicar el proyecto: el script SQL, las dos
unidades de systemd, la configuración de Nginx como proxy inverso y las plantillas de variables
de entorno. El procedimiento paso a paso está en
[`documentacion/DESPLIEGUE.md`](documentacion/DESPLIEGUE.md).

En producción Nginx es el único puerto público. Los dos microservicios escuchan solo en
`127.0.0.1` y se alcanzan por prefijo:

- `/api/auth-service/` → `127.0.0.1:8081/api/`
- `/api/citas-service/` → `127.0.0.1:8082/api/`

## Estructura del repositorio

```
nutrivida-auth/      microservicio de autenticación y usuarios
nutrivida-citas/     microservicio de citas, catálogo y fichas clínicas
despliegue/          script SQL, unidades systemd, Nginx y plantillas de entorno
documentacion/       documentación de los endpoints y del despliegue
```
