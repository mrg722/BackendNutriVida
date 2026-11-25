# Documentación de APIs · Clínica NutriVida

**Versión del documento:** 1.0
**Asignatura:** DSY1104 Desarrollo FullStack II · Evaluación Parcial 3
**Caso:** Forma D · Clínica Nutricional NutriVida

| Dato | Valor |
|---|---|
| URL API de autenticación | `http://localhost:8081/api/v1` |
| URL Swagger de autenticación | `http://localhost:8081/swagger-ui/index.html` |
| URL API de citas | `http://localhost:8082/api/v1` |
| URL Swagger de citas | `http://localhost:8082/swagger-ui/index.html` |
| Tipo de API | Privada, salvo el inicio de sesión y el registro público |
| Formato | JSON en la petición y en la respuesta, incluidos los errores |
| Autenticación | JWT en el encabezado `Authorization: Bearer <token>` |
| Versionado | Prefijo `/api/v1` en todas las rutas |

Las rutas públicas en producción pasan por Nginx y quedan como
`/api/auth-service/v1/...` y `/api/citas-service/v1/...`.

## Roles del sistema

| Rol | Descripción |
|---|---|
| `ADMIN` | Administrador de la clínica. Acceso total. |
| `NUTRICIONISTA` | Profesional que atiende. Ve su agenda y escribe las fichas que registra. |
| `PACIENTE` | Persona que asiste a controles. Gestiona sus citas y lee su historial. |

## Códigos de respuesta usados

| Código | Cuándo lo devuelvo |
|---|---|
| 200 | La operación de lectura o actualización resultó correcta |
| 201 | Se creó un recurso nuevo |
| 204 | La eliminación resultó correcta y no hay cuerpo que devolver |
| 400 | El cuerpo no cumple las validaciones, o el dato está fuera de lo permitido |
| 401 | No hay token, o el token está vencido, alterado o emitido por otra aplicación |
| 403 | El token es válido pero el rol no autoriza esa operación, o el dato no le pertenece |
| 404 | El recurso pedido no existe |
| 409 | El dato es válido pero choca con una regla de negocio |

Todos los errores tienen la misma forma:

```json
{ "error": "Una atención de 50 minutos a las 16:30 no cabe en el horario de Nut. Carolina Fuentes M., que atiende de 09:00 a 17:00" }
```

---

# Microservicio nutrivida-auth

Puerto 8081, base de datos `nutrivida_auth`. Es el único que firma tokens.

## Endpoint de autenticación

### POST /api/v1/auth/login

| Campo | Detalle |
|---|---|
| Descripción | Comprueba las credenciales y devuelve un token JWT con el rol |
| Datos de entrada | `{"correo": "admin@nutrivida.cl", "password": "Cambiar123!"}` |
| Respuestas | 200 (correcto), 400 (datos inválidos), 401 (credenciales inválidas) |
| Requiere autenticación | No |
| Roles permitidos | N/A |
| Observaciones | Una cuenta desactivada también recibe 401, con el mismo mensaje, para no revelar su estado |

Respuesta correcta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "correo": "admin@nutrivida.cl",
  "nombre": "Administrador NutriVida",
  "rol": "ADMIN"
}
```

El token lleva los claims `iss` (siempre `nutrivida-auth`), `sub` con el correo, `rol`, `nombre`,
`iat` y `exp`. Vive una hora.

### POST /api/v1/auth/registro

| Campo | Detalle |
|---|---|
| Descripción | Crea una cuenta nueva desde el formulario público de la web |
| Datos de entrada | `{"nombre": "Ana Pérez", "correo": "ana@gmail.com", "password": "Clave123"}` |
| Respuestas | 201 (creado), 400 (datos inválidos), 409 (el correo ya existe) |
| Requiere autenticación | No |
| Roles permitidos | N/A |
| Observaciones | El rol siempre queda en `PACIENTE`. El cuerpo no puede elegirlo, así nadie se crea una cuenta de administrador |

### GET /api/v1/auth/perfil

| Campo | Detalle |
|---|---|
| Descripción | Devuelve los datos del usuario dueño del token |
| Datos de entrada | Ninguno |
| Respuestas | 200 (correcto), 401 (token ausente, vencido o alterado) |
| Requiere autenticación | Sí |
| Roles permitidos | Los tres |
| Observaciones | El frontend lo llama al recargar la página para revalidar la sesión guardada |

### POST /api/v1/auth/renovar

| Campo | Detalle |
|---|---|
| Descripción | Entrega un token nuevo a quien ya trae uno válido, sin volver a pedir la contraseña |
| Datos de entrada | Ninguno. La credencial es el token del encabezado `Authorization` |
| Respuestas | 200 (token nuevo), 401 (token ausente, vencido o alterado) |
| Requiere autenticación | Sí |
| Roles permitidos | Los tres |
| Observaciones | La respuesta tiene la misma forma que la del login |

Devuelve lo mismo que `/auth/login`: `token`, `correo`, `nombre` y `rol`.

Dos detalles de seguridad de este endpoint:

- Nunca renuevo a partir de un token vencido o alterado. El filtro valida la firma, el emisor y
  la expiración antes de que la petición llegue al controlador, así que un token caducado recibe
  401 y no un token nuevo.
- Releo la cuenta desde la base de datos en vez de confiar en los datos del token anterior. Si el
  administrador cambió el rol, el token nuevo lleva el rol actual; y si desactivó la cuenta, la
  renovación se rechaza aunque el token viejo siguiera vigente.

## Endpoint de usuarios

Es el CRUD del panel de administración. Todas las operaciones exigen rol `ADMIN`: un token de
`PACIENTE` o de `NUTRICIONISTA` recibe 403.

| Método | Ruta | Descripción | Datos de entrada | Respuestas | Roles |
|---|---|---|---|---|---|
| GET | `/api/v1/usuarios` | Lista los usuarios. Acepta `?rol=NUTRICIONISTA` | Ninguno | 200, 401, 403 | `ADMIN` |
| GET | `/api/v1/usuarios/{id}` | Busca un usuario por su id | Ninguno | 200, 401, 403, 404 | `ADMIN` |
| POST | `/api/v1/usuarios` | Crea un usuario y le asigna un rol | `{"nombre","correo","password","rol","activo"}` | 201, 400, 401, 403, 409 | `ADMIN` |
| PUT | `/api/v1/usuarios/{id}` | Edita el usuario, cambia su rol o lo desactiva | El mismo cuerpo, con `password` opcional | 200, 400, 401, 403, 404, 409 | `ADMIN` |
| DELETE | `/api/v1/usuarios/{id}` | Elimina el usuario definitivamente | Ninguno | 204, 401, 403, 404 | `ADMIN` |

Observaciones:

- En `PUT`, si `password` llega vacío conservo la contraseña anterior. Así editar el nombre no
  obliga a cambiar la clave.
- `rol` solo acepta `ADMIN`, `NUTRICIONISTA` o `PACIENTE`. Cualquier otro valor da 400.
- `activo` en `false` desactiva la cuenta sin borrarla: el usuario deja de poder iniciar sesión.
- Ninguna respuesta incluye el hash de la contraseña.

---

# Microservicio nutrivida-citas

Puerto 8082, base de datos `nutrivida_citas`. No firma tokens: solo verifica la firma y el
emisor del token que recibe.

## Endpoint de servicios

Es el catálogo de la clínica. Lo lee cualquier sesión válida y lo modifica solo `ADMIN`.

| Método | Ruta | Descripción | Datos de entrada | Respuestas | Roles |
|---|---|---|---|---|---|
| GET | `/api/v1/servicios` | Lista el catálogo. Acepta `?soloActivos=true` y `?tipo=Consulta` | Ninguno | 200, 401 | Los tres |
| GET | `/api/v1/servicios/{id}` | Busca un servicio por su id | Ninguno | 200, 401, 404 | Los tres |
| POST | `/api/v1/servicios` | Crea un servicio en el catálogo | `{"codigo","tipo","nombre","duracionMinutos","modalidad","precio","descripcion","activo"}` | 201, 400, 401, 403, 409 | `ADMIN` |
| PUT | `/api/v1/servicios/{id}` | Actualiza un servicio | El mismo cuerpo | 200, 400, 401, 403, 404, 409 | `ADMIN` |
| DELETE | `/api/v1/servicios/{id}` | Elimina un servicio | Ninguno | 204, 401, 403, 404 | `ADMIN` |

Observaciones:

- `codigo` es único. Repetirlo da 409.
- `precio` va en pesos chilenos, como entero y nunca negativo.
- `duracionMinutos` puede ser `null`: los planes mensuales no tienen duración por sesión.
- Tipos cargados: `Consulta`, `Plan especializado`, `Evaluación` y `Taller grupal`.
- `activo` en `false` retira el servicio del catálogo sin perder las citas que ya lo usaron.

## Endpoint de nutricionistas

| Método | Ruta | Descripción | Datos de entrada | Respuestas | Roles |
|---|---|---|---|---|---|
| GET | `/api/v1/nutricionistas` | Lista el directorio. Acepta `?soloActivos=true` | Ninguno | 200, 401 | Los tres |
| GET | `/api/v1/nutricionistas/{id}` | Busca un profesional por su id | Ninguno | 200, 401, 404 | Los tres |
| POST | `/api/v1/nutricionistas` | Agrega un profesional al directorio | `{"codigo","nombre","correo","especialidad","diasAtencion","horario","activo"}` | 201, 400, 401, 403, 409 | `ADMIN` |
| PUT | `/api/v1/nutricionistas/{id}` | Actualiza sus datos o su disponibilidad | El mismo cuerpo | 200, 400, 401, 403, 404, 409 | `ADMIN` |
| DELETE | `/api/v1/nutricionistas/{id}` | Elimina un profesional del directorio | Ninguno | 204, 401, 403, 404 | `ADMIN` |

Observaciones:

- `codigo` y `correo` son únicos. El `correo` es el mismo con el que el profesional inicia
  sesión en `nutrivida-auth`: es el vínculo entre su cuenta y su agenda, porque no puedo poner
  una llave foránea entre dos bases de datos distintas.
- El formulario de agendamiento usa `?soloActivos=true` para no ofrecer a alguien que ya no atiende.

## Endpoint de citas

Es el flujo del "pedido" de este caso.

| Método | Ruta | Descripción | Datos de entrada | Respuestas | Roles |
|---|---|---|---|---|---|
| GET | `/api/v1/citas/mias` | Devuelve las citas que corresponden al rol de la sesión | Ninguno | 200, 401 | Los tres |
| GET | `/api/v1/citas` | Lista todas las citas de la clínica | Ninguno | 200, 401, 403 | `ADMIN` |
| GET | `/api/v1/citas/{id}` | Busca una cita por su id | Ninguno | 200, 401, 403, 404 | Dueño, su nutricionista o `ADMIN` |
| POST | `/api/v1/citas` | Solicita una hora nueva | `{"nutricionistaId","servicioId","fecha","hora","motivo"}` | 201, 400, 401, 403, 404, 409 | `PACIENTE`, `ADMIN` |
| PUT | `/api/v1/citas/{id}` | Reprograma la cita | El mismo cuerpo | 200, 400, 401, 403, 404, 409 | Dueño, su nutricionista o `ADMIN` |
| PUT | `/api/v1/citas/{id}/estado` | Confirma, atiende, cancela o marca inasistencia | `{"estado": "CONFIRMADA"}` | 200, 400, 401, 403, 404, 409 | Dueño, su nutricionista o `ADMIN` |
| DELETE | `/api/v1/citas/{id}` | Elimina la cita del historial | Ninguno | 204, 401, 403, 404 | `ADMIN` |

Cuerpo de ejemplo para solicitar una hora:

```json
{
  "nutricionistaId": 1,
  "servicioId": 1,
  "fecha": "2026-11-26",
  "hora": "10:00",
  "motivo": "Primera consulta para control de peso"
}
```

Respuesta:

```json
{
  "id": 1,
  "correoPaciente": "paciente@nutrivida.cl",
  "nutricionistaId": 1,
  "nutricionistaNombre": "Nut. Carolina Fuentes M.",
  "nutricionistaEspecialidad": "Obesidad y síndrome metabólico",
  "servicioId": 1,
  "servicioCodigo": "CN001",
  "servicioNombre": "Primera consulta nutricional",
  "servicioPrecio": 35000,
  "fecha": "2026-11-26",
  "hora": "10:00:00",
  "motivo": "Primera consulta para control de peso",
  "estado": "SOLICITADA"
}
```

### Estados de la cita

| Estado | Significado |
|---|---|
| `SOLICITADA` | El paciente pidió la hora y la clínica todavía no la revisa |
| `CONFIRMADA` | La clínica aceptó el horario |
| `ATENDIDA` | La cita ocurrió |
| `CANCELADA` | Se dio de baja antes de ocurrir |
| `NO_ASISTIO` | El paciente no llegó. Es el estado con el que calculo la tasa de inasistencia |

Observaciones del flujo:

- El correo del paciente lo tomo del token, nunca del cuerpo. Nadie puede agendar a nombre de otro.
- Una cita nueva siempre nace en `SOLICITADA`: nadie se autoconfirma.
- Un `PACIENTE` solo puede llevar su cita a `CANCELADA`. Cualquier otro estado le da 403.
- `ATENDIDA`, `CANCELADA` y `NO_ASISTIO` cierran la cita: cualquier cambio posterior da 409.
- Reprogramar devuelve la cita a `SOLICITADA`, porque la clínica tiene que confirmar el horario nuevo.
- `SOLICITADA` y `CONFIRMADA` ocupan el horario. Para saber si una hora está libre comparo los
  bloques completos, no la hora de inicio: la duración del servicio dice hasta cuándo queda
  ocupado el profesional, así que una consulta de 50 minutos a las 10:00 impide tomar un control
  a las 10:30. Si la hora choca, respondo 409. Una cita cancelada libera el cupo.
- La fecha no puede ser anterior a hoy, el día tiene que ser uno de los que ese profesional
  atiende, y la cita completa tiene que caber dentro de su horario: no basta con que empiece
  dentro de la jornada, también tiene que terminar antes del cierre. Los tres casos dan 400.
- El motivo necesita entre 10 y 300 caracteres.

## Endpoint de mediciones

Es la ficha clínica digital. Aquí se aplica la restricción de privacidad del caso: peso, medidas
y diagnósticos son datos sensibles.

| Método | Ruta | Descripción | Datos de entrada | Respuestas | Roles |
|---|---|---|---|---|---|
| GET | `/api/v1/mediciones/mias` | Historial del paciente de la sesión | Ninguno | 200, 401 | `PACIENTE` |
| GET | `/api/v1/mediciones/registradas` | Mediciones que registró el profesional de la sesión | Ninguno | 200, 401 | `NUTRICIONISTA` |
| GET | `/api/v1/mediciones/paciente/{correo}` | Ficha de un paciente concreto | Ninguno | 200, 401, 403 | Dueño, su nutricionista o `ADMIN` |
| GET | `/api/v1/mediciones/{id}` | Busca una medición por su id | Ninguno | 200, 401, 403, 404 | Dueño, autor o `ADMIN` |
| POST | `/api/v1/mediciones` | Registra una medición en la ficha | `{"correoPaciente","fecha","pesoKg","tallaCm","cinturaCm","observaciones","planAlimenticio"}` | 201, 400, 401, 403 | `NUTRICIONISTA`, `ADMIN` |
| PUT | `/api/v1/mediciones/{id}` | Corrige una medición | El mismo cuerpo | 200, 400, 401, 403, 404 | Autor o `ADMIN` |
| DELETE | `/api/v1/mediciones/{id}` | Elimina la medición | Ninguno | 204, 401, 403, 404 | Autor o `ADMIN` |

Respuesta de ejemplo:

```json
{
  "id": 1,
  "correoPaciente": "paciente@nutrivida.cl",
  "correoNutricionista": "carolina.fuentes@nutrivida.cl",
  "fecha": "2026-11-16",
  "pesoKg": 82.5,
  "tallaCm": 172.0,
  "cinturaCm": 95.0,
  "imc": 27.9,
  "observaciones": "Primer control",
  "planAlimenticio": "Cuatro comidas diarias"
}
```

Observaciones:

- El correo del nutricionista lo tomo del token: la ficha queda firmada por quien la escribió.
- El `imc` no existe como columna. Es un dato derivado del peso y la talla, y lo calculo al
  responder para no guardar información repetida en la base.
- Un `PACIENTE` solo accede a su propia ficha, incluso si escribe otro correo en la URL.
- Un `NUTRICIONISTA` solo ve las mediciones que él mismo registró, así que no accede a los datos
  clínicos de pacientes de otros profesionales de la clínica.
- Modificar o eliminar una medición queda reservado a su autor y al administrador.
- `pesoKg` admite de 1 a 400, `tallaCm` de 30 a 250 y `cinturaCm` de 20 a 250. Fuera de rango da 400.
- `planAlimenticio` es el plan que antes se entregaba impreso y el paciente podía perder.

## Endpoint de estadísticas

| Método | Ruta | Descripción | Datos de entrada | Respuestas | Roles |
|---|---|---|---|---|---|
| GET | `/api/v1/estadisticas` | Resumen de citas, inasistencia y carga por profesional | Ninguno | 200, 401, 403 | `ADMIN` |

```json
{
  "totalCitas": 1,
  "citasPorEstado": {
    "SOLICITADA": 0, "CONFIRMADA": 0, "ATENDIDA": 1, "CANCELADA": 0, "NO_ASISTIO": 0
  },
  "tasaInasistencia": 0.0,
  "atencionesPorNutricionista": [
    { "nutricionistaId": 1, "nombre": "Nut. Carolina Fuentes M.", "total": 1 }
  ]
}
```

Observaciones: `tasaInasistencia` es el porcentaje de citas en `NO_ASISTIO` sobre el total,
redondeado a un decimal. Resuelve el problema del caso de no tener estadísticas sin revisar las
agendas página por página.

---

## Cómo probar los endpoints protegidos en Swagger

1. Abro `http://localhost:8081/swagger-ui/index.html`.
2. Ejecuto `POST /api/v1/auth/login` con una de las cuentas iniciales.
3. Copio el valor de `token`, sin las comillas.
4. Abro `http://localhost:8082/swagger-ui/index.html`.
5. Presiono **Authorize**, pego el token y confirmo.
6. Ejecuto cualquier endpoint. Si cierro Authorize o pego un token alterado, responde 401.

Que la página de Swagger sea pública no significa que los endpoints lo sean: cada petición sigue
pasando por `JwtAuthenticationFilter` y por las reglas de `SecurityConfig`.
