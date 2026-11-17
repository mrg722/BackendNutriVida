# Decisiones técnicas del proyecto

Dejo aquí por escrito las decisiones de arquitectura y despliegue que tomé, con el motivo de
cada una y las alternativas que descarté. Lo escribo para poder sostenerlas en la defensa del
proyecto y para que quien lea el repositorio entienda por qué está construido así.

---

## 1. Por qué dos microservicios y no una sola aplicación

El caso exige una arquitectura de microservicios. La dividí por responsabilidad de negocio, que
es el criterio correcto, no por capas técnicas:

- `nutrivida-auth` es dueño de las personas: usuarios, contraseñas, roles y emisión del token.
- `nutrivida-citas` es dueño de la operación de la clínica: catálogo, agenda, fichas y estadísticas.

Cada uno tiene su propio `pom.xml`, su propio puerto, su propia base de datos y su propio
proceso. **No comparten clases ni tablas.** `nutrivida-citas` no conoce la clase `Usuario` ni
puede leer la tabla `usuarios`.

La única relación entre ambos es el token JWT: Auth lo firma y Citas verifica localmente la firma
y el emisor. Esa verificación es local, con la clave compartida, así que **Citas no hace una
llamada de red a Auth en cada petición**. Si Auth se cayera, Citas seguiría autorizando
correctamente a quien ya tenga un token válido.

Una consecuencia de esta separación que vale la pena explicar: el paciente **no tiene tabla** en
la base de citas. Su identidad es el correo que viaja dentro del token, porque sus datos
personales son responsabilidad del otro microservicio. No se puede poner una llave foránea entre
dos bases de datos distintas, y tampoco se debe: eso rompería la independencia.

## 2. Por qué el IMC no está guardado en la base de datos

La tabla `mediciones` guarda peso, talla y cintura, pero **no guarda el IMC**, aunque la
aplicación lo muestra en todas partes.

El IMC es un dato derivado: se calcula dividiendo el peso en kilos por la talla en metros al
cuadrado. Guardarlo sería almacenar información que ya está contenida en otras dos columnas, y
eso rompe la tercera forma normal. Peor aún en la práctica: si alguien corrigiera el peso de una
medición y el IMC guardado no se recalculara, la ficha quedaría mostrando dos datos que se
contradicen.

Lo calculo al momento de responder, en `MedicionResponse`, y lo probé con sus casos límite
(talla nula y talla cero, para no dividir por cero).

## 3. Por qué las rutas van versionadas

Todas las rutas llevan el prefijo `/api/v1`. No es decoración: permite publicar una versión 2 de
un endpoint más adelante sin romper a quien ya consume la versión 1. Es una práctica estándar de
diseño de APIs REST y es barata de adoptar al principio, pero carísima de agregar después, cuando
ya hay clientes conectados.

## 4. Por qué el rol viaja dentro del token

El token JWT lleva el correo en el `subject` y el rol en un claim. Eso permite que
`nutrivida-citas` decida si autoriza una operación **sin consultar la base de usuarios**, que
pertenece al otro microservicio.

El riesgo conocido de esta decisión es que el token refleja el rol que tenía la persona cuando
inició sesión. Si el administrador le cambia el rol, el token viejo sigue diciendo lo anterior
hasta que expira. Lo acoté de dos maneras: el token dura una hora, y la **renovación** vuelve a
leer la cuenta desde la base de datos, así que al renovar se toma el rol actual y una cuenta
desactivada ya no puede renovar.

## 5. Por qué no usé Docker

El caso menciona Docker en su tabla de stack tecnológico. Decidí no usarlo, y el motivo es este:

Las guías del curso que describen el despliegue (la que trabaja AWS) enseñan a desplegar con
**systemd y Nginx**, sin contenedores, y ese es el procedimiento que seguí y documenté. Agregar
Docker habría significado introducir una capa que ninguna guía enseña, que no podría probar en mi
equipo (no tengo Docker instalado ni WSL2), y entregar archivos `Dockerfile` que nadie ejecutó
nunca. Un archivo de despliegue sin probar es peor que no tenerlo, porque da una seguridad falsa.

Qué hace cada cosa, para poder explicarlo:

- **systemd** es el administrador de servicios de Linux. Mantiene los procesos vivos, los arranca
  al encender el servidor y los reinicia si fallan. Cada microservicio tiene su propia unidad, así
  que uno se puede reiniciar sin tocar al otro.
- **Nginx** es un proxy inverso. Recibe todo el tráfico público en el puerto 80, entrega los
  archivos estáticos de React y reenvía cada grupo de rutas al microservicio que corresponde.
- **Docker** empaqueta la aplicación junto con sus dependencias en una imagen aislada. Aporta
  reproducibilidad y portabilidad, pero es una capa adicional que este proyecto no necesita para
  funcionar.

systemd y Nginx cubren lo mismo que se necesita aquí: procesos que se mantienen encendidos y un
punto de entrada público único. Docker habría aportado reproducibilidad, que es valiosa en un
equipo grande, pero no resuelve ningún problema que este proyecto tenga hoy.

## 6. Por qué descarté AWS Academy Learner Lab

El caso pide desplegar en AWS y la institución entrega acceso a AWS Academy Learner Lab. Lo
descarté, y no por comodidad: el laboratorio tiene cuatro características que lo hacen poco
confiable para una demostración en una fecha y hora fijas.

1. **Las instancias quedan detenidas entre sesiones.** El laboratorio apaga las máquinas EC2 al
   cerrar la sesión. Antes de la presentación hay que entrar al campus, iniciar el laboratorio y
   esperar a que arranquen la instancia y los servicios. Son entre 5 y 15 minutos durante los
   cuales la dirección no responde.
2. **La dirección IP pública cambia en cada arranque.** La solución es asignar una IP elástica,
   pero no pude confirmar en la documentación oficial que el laboratorio lo permita: hay
   documentación de AWS que la menciona, una guía universitaria que la documenta paso a paso, y
   material de instructores que diseña sus laboratorios asumiendo que no está disponible. La
   solución al problema principal es ella misma una incógnita.
3. **La sesión se corta a las 4 horas sin aviso.** Si la evaluación se atrasa o se extiende, las
   instancias se detienen en medio de la demostración.
4. **Agotar el presupuesto borra todo y desactiva la cuenta**, y el contador de gasto se
   actualiza con hasta 8 horas de retraso, así que no hay forma fiable de anticiparlo.

Para un entorno de aprendizaje donde se puede volver a intentar, nada de esto importa. Para una
evaluación con nota, en una hora concreta y sin segunda oportunidad, es demasiado riesgo
concentrado en un solo punto de falla.

Dejé igualmente escrito y completo todo el procedimiento de despliegue sobre una instancia EC2
(ver `DESPLIEGUE.md` y la carpeta `despliegue/`), porque es el camino que enseña el curso y
porque sigue siendo válido: lo que descarté fue depender de él el día de la evaluación, no el
procedimiento en sí.

## 7. Qué exige realmente la evaluación sobre el despliegue

Conviene tener claro qué está en juego y qué no:

- **La rúbrica no evalúa el despliegue.** Ninguno de sus indicadores menciona Docker, AWS ni una
  dirección pública. Evalúa el backend con base de datos, la API REST documentada en Swagger, la
  integración, la autenticación con JWT y roles, la gestión de sesión y las restricciones de
  acceso.
- **La lista de entregables tampoco lo pide.** Pide los dos enlaces de GitHub, los proyectos
  comprimidos y cuatro documentos. No pide una URL en línea.
- **El despliegue sí es necesario por una razón práctica**: la presentación se hace en un
  computador que no tiene instalado nada del entorno de desarrollo, así que la única forma de
  demostrar el sistema funcionando es que esté accesible desde un navegador.

Esa distinción es importante: desplegué por una necesidad operativa de la presentación, no
porque la rúbrica lo exija.
