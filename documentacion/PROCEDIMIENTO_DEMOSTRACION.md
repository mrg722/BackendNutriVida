# Procedimiento para la demostración

Esto es lo que tengo que hacer el día de la presentación, en orden. Lo escribo porque con los
nervios del momento no quiero estar recordando comandos ni direcciones.

El modelo es simple: **mi computador es el servidor y el de la sala es solo el navegador.** En
el computador del profesor no se instala ni se ejecuta nada; abre una dirección y ya.

---

## La noche anterior

Hago el ensayo completo en frío. Es la única forma de saber cuánto tarda de verdad en estar
listo y de descubrir cualquier problema cuando todavía hay tiempo de arreglarlo.

1. Apago todo: cierro las ventanas de los servicios o reinicio el equipo.
2. Ejecuto `despliegue\arrancar-demostracion.bat` y **cronometro** cuánto pasa hasta que la
   página responde. Ese número es el margen que tengo que reservar al día siguiente.
3. Abro la dirección local y recorro el flujo completo: inicio sesión con los tres perfiles,
   pido una cita, la confirmo desde la agenda, registro una medición y reviso las estadísticas.
4. Grabo el video de respaldo con **Win+G**, mostrando ese mismo recorrido.
5. Dejo el computador cargado y con la suspensión desactivada.

## Una hora antes

1. Enciendo el equipo y ejecuto `despliegue\arrancar-demostracion.bat`.
2. Anoto la **dirección de red** que me muestra al terminar. Cambia según la red a la que esté
   conectado, así que la de ayer no me sirve.
3. Abro esa dirección en mi propio navegador para confirmar que responde.
4. Desactivo la suspensión de la pantalla y del equipo. Si la pantalla se apaga en medio de la
   presentación, el servidor sigue, pero no quiero arriesgar nada.

## Al llegar a la sala

1. Conecto mi computador a la red de la sala.
2. Vuelvo a ejecutar el `.bat`. No pasa nada si algo ya estaba corriendo: el archivo lo detecta
   y no lo lanza dos veces.
3. **Anoto la dirección de red nueva**, porque al cambiar de red cambia la IP.
4. Desde el computador del profesor abro esa dirección. Si carga, estoy listo.

---

## Si la dirección de red no funciona

Es el caso más probable de falla: muchas redes institucionales **aíslan los equipos entre sí**
por seguridad, así que el computador del profesor no ve el mío aunque estén en la misma WiFi.
No es un error mío ni del sistema.

Para eso está el túnel. En una terminal:

```
C:\DuocTrabajos\herramientas\cloudflared.exe tunnel --url http://localhost:4173
```

A los pocos segundos imprime una dirección que termina en `.trycloudflare.com`. Esa es pública:
funciona desde cualquier computador con internet, aunque la red aísle los equipos, porque la
conexión sale hacia afuera en lugar de ir de una máquina a otra.

Dos cosas que tengo que recordar del túnel:

- **La dirección cambia cada vez que lo levanto.** La genero en el momento y la anoto; no sirve
  apuntarla con días de anticipación.
- **Necesita internet.** Si la WiFi de la sala falla, comparto los datos de mi teléfono y vuelvo
  a levantarlo.

## Si tampoco hay internet

Reproduzco la grabación. No es lo ideal, pero conviene tener presente que **la rúbrica evalúa
cómo explico, no si la aplicación arranca**: los seis indicadores de la presentación piden
describir, explicar, justificar y exponer. Con el video puedo recorrer el sistema y explicar
cada punto igual.

---

## Direcciones y cuentas

Con el sistema arriba:

| Qué | Dónde |
|---|---|
| La aplicación, en mi equipo | `http://localhost:4173` |
| La aplicación, desde otro computador | `http://MI_IP:4173` (la muestra el `.bat`) |
| Swagger de autenticación | `http://localhost:8081/swagger-ui/index.html` |
| Swagger de citas | `http://localhost:8082/swagger-ui/index.html` |

Cuentas para la demostración, todas con la contraseña `Cambiar123!`:

| Correo | Rol | Para mostrar |
|---|---|---|
| `admin@nutrivida.cl` | `ADMIN` | Panel de usuarios, catálogo y estadísticas |
| `carolina.fuentes@nutrivida.cl` | `NUTRICIONISTA` | Agenda y ficha clínica |
| `paciente@nutrivida.cl` | `PACIENTE` | Pedir cita, mis citas y mi progreso |

---

## Qué revisar si algo no responde

| Síntoma | Dónde mirar |
|---|---|
| La página no abre | ¿Está el puerto 4173 corriendo? Revisar `mysql-datos\preview.log` |
| La página abre pero no carga datos | Los microservicios no están arriba. Revisar `auth-error.log` y `citas-error.log` |
| Todo responde 401 al iniciar sesión | MySQL no arrancó, así que no hay usuarios que comprobar |
| El otro computador no ve nada | La red aísla los equipos: paso al túnel |
| El túnel no levanta | No hay internet: comparto datos del teléfono |

Un detalle que conviene recordar: los dos microservicios **tardan unos segundos en estar
listos** después de arrancar. El `.bat` ya espera por ellos antes de publicar la página, pero si
abro la dirección demasiado pronto puedo ver un error que se arregla solo recargando.
