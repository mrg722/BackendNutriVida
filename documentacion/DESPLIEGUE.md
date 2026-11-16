# Despliegue en AWS EC2

Procedimiento de despliegue de los dos microservicios y del frontend en una instancia EC2.
Los archivos que menciono están en la carpeta `despliegue/` de este repositorio.

## 1. Arquitectura de producción

| Componente | Ubicación | Puerto | Acceso |
|---|---|---|---|
| Nginx y React | EC2 | 80 | Público |
| `nutrivida-auth` | EC2 | 8081 | Solo `127.0.0.1` |
| `nutrivida-citas` | EC2 | 8082 | Solo `127.0.0.1` |
| MySQL | EC2 | 3306 | Solo local |
| SSH | EC2 | 22 | Solo mi IP |

En el grupo de seguridad de EC2 abro únicamente el 22 desde mi IP y el 80 desde Internet. Los
puertos 8081, 8082 y 3306 quedan cerrados: Nginx es el único punto HTTP público.

| Ruta pública | Destino interno |
|---|---|
| `/api/auth-service/**` | `127.0.0.1:8081/api/**` |
| `/api/citas-service/**` | `127.0.0.1:8082/api/**` |

El resto de las rutas las atienden los archivos de React publicados en `/var/www/nutrivida`.

## 2. Preparar la instancia

```bash
sudo apt update
sudo apt install -y mysql-server openjdk-21-jre-headless nginx curl
sudo systemctl enable --now mysql nginx
java -version
```

## 3. Crear las bases de datos

Copio el script al servidor y lo ejecuto. Antes reemplazo `CAMBIAR_CLAVE_AUTH` y
`CAMBIAR_CLAVE_CITAS` por claves reales.

```bash
sudo mysql < 01-crear-bases-y-usuarios.sql
```

Comprobación: `SHOW DATABASES LIKE 'nutrivida%';` debe mostrar las dos bases, y las dos cuentas
deben poder conectarse solo desde `localhost`.

## 4. Generar el secreto compartido del token

Lo genero una sola vez y uso el mismo valor en los dos archivos de entorno. Si no coinciden, el
inicio de sesión funcionará pero todas las peticiones a citas responderán 401.

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

## 5. Archivos de entorno protegidos

Copio `auth.env.example` y `citas.env.example`, les pongo los valores reales y los dejo fuera
del alcance de cualquier otro usuario del sistema.

```bash
sudo mkdir -p /etc/nutrivida
sudo cp auth.env.example /etc/nutrivida/auth.env
sudo cp citas.env.example /etc/nutrivida/citas.env
sudo nano /etc/nutrivida/auth.env
sudo nano /etc/nutrivida/citas.env
sudo chown root:root /etc/nutrivida/*.env
sudo chmod 600 /etc/nutrivida/*.env
```

En `APP_CORS_ALLOWED_ORIGIN` pongo el origen exacto del frontend, por ejemplo
`http://IP_PUBLICA`. Si no calza carácter por carácter, el navegador bloquea las peticiones
por CORS.

## 6. Compilar y copiar los JAR

Desde mi computador, en cada proyecto:

```powershell
.\mvnw.cmd clean package
scp -i "RUTA\CLAVE.pem" target\nutrivida-auth-0.0.1-SNAPSHOT.jar ubuntu@IP_PUBLICA:/tmp/nutrivida-auth.jar
scp -i "RUTA\CLAVE.pem" target\nutrivida-citas-0.0.1-SNAPSHOT.jar ubuntu@IP_PUBLICA:/tmp/nutrivida-citas.jar
```

Les pongo nombres distintos a propósito, para no sobrescribir un microservicio con el otro.

## 7. Instalar los servicios de systemd

```bash
sudo cp nutrivida-auth.service nutrivida-citas.service /etc/systemd/system/
sudo mkdir -p /opt/nutrivida-auth /opt/nutrivida-citas
sudo mv /tmp/nutrivida-auth.jar /opt/nutrivida-auth/app.jar
sudo mv /tmp/nutrivida-citas.jar /opt/nutrivida-citas/app.jar
sudo chown -R ubuntu:ubuntu /opt/nutrivida-auth /opt/nutrivida-citas
sudo systemctl daemon-reload
sudo systemctl enable --now nutrivida-auth nutrivida-citas
sudo systemctl status nutrivida-auth --no-pager
sudo systemctl status nutrivida-citas --no-pager
```

Las dos unidades deben quedar en `active (running)`. Son procesos separados: si uno falla,
systemd reinicia solo ese.

## 8. Comprobar los backend antes de Nginx

```bash
curl -i http://127.0.0.1:8081/api/v1/auth/login
curl -i http://127.0.0.1:8082/api/v1/servicios
sudo ss -lntp | grep -E '8081|8082'
```

| Comprobación | Respuesta esperada |
|---|---|
| GET al login | 405, porque el endpoint solo acepta POST |
| GET a servicios sin token | 401 |
| `ss` en 8081 y 8082 | Java escuchando en `127.0.0.1`, no en `0.0.0.0` |

## 9. Publicar el frontend

En el repositorio del frontend creo `.env.production` con las rutas relativas que atiende Nginx,
compilo y copio el resultado:

```bash
npm install
npm run build
scp -i "RUTA\CLAVE.pem" -r dist/* ubuntu@IP_PUBLICA:/tmp/nutrivida-web/
```

En el servidor:

```bash
sudo mkdir -p /var/www/nutrivida
sudo cp -a /tmp/nutrivida-web/. /var/www/nutrivida/
sudo chown -R www-data:www-data /var/www/nutrivida
sudo find /var/www/nutrivida -type d -exec chmod 755 {} \;
sudo find /var/www/nutrivida -type f -exec chmod 644 {} \;
```

## 10. Configurar Nginx

```bash
sudo cp nginx-nutrivida.conf /etc/nginx/sites-available/nutrivida
sudo ln -sfn /etc/nginx/sites-available/nutrivida /etc/nginx/sites-enabled/nutrivida
sudo unlink /etc/nginx/sites-enabled/default 2>/dev/null || true
sudo nginx -t
sudo systemctl reload nginx
```

La barra final en `location` y en `proxy_pass` es la que reemplaza el prefijo público por
`/api/`. El bloque `try_files` es lo que permite recargar una ruta interna de React sin recibir
un 404.

## 11. Probar el flujo completo

```bash
curl -i -X POST "http://IP_PUBLICA/api/auth-service/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"correo":"admin@nutrivida.cl","password":"Cambiar123!"}'

curl -i "http://IP_PUBLICA/api/citas-service/v1/citas" \
  -H "Authorization: Bearer TOKEN_COPIADO"
```

| Caso | Resultado esperado |
|---|---|
| Inicio de sesión válido | 200 con el token |
| Inicio de sesión incorrecto | 401 |
| Citas sin token | 401 |
| Citas con token de `PACIENTE` | 403, porque el listado completo es solo de `ADMIN` |
| Citas con token de `ADMIN` | 200 |
| Token alterado | 401 |

## 12. Revisar Swagger sin abrir puertos

Levanto un túnel SSH y dejo la terminal abierta. Los puertos 8081 y 8082 siguen cerrados en el
grupo de seguridad.

```powershell
ssh -i "RUTA\CLAVE.pem" -L 18081:127.0.0.1:8081 -L 18082:127.0.0.1:8082 ubuntu@IP_PUBLICA
```

Después abro `http://localhost:18081/swagger-ui/index.html` y
`http://localhost:18082/swagger-ui/index.html`.

## 13. Diagnóstico

```bash
sudo nginx -t
sudo systemctl status nutrivida-auth --no-pager
sudo journalctl -u nutrivida-auth -n 80 --no-pager
sudo journalctl -u nutrivida-citas -n 80 --no-pager
sudo tail -n 80 /var/log/nginx/error.log
```

| Síntoma | Primera revisión |
|---|---|
| 502 Bad Gateway | El servicio está detenido o el `proxy_pass` apunta al puerto equivocado |
| 401 en citas con token recién emitido | `JWT_SECRET` distinto entre los dos archivos de entorno, o emisor diferente |
| 403 en una operación de administración | El token es válido pero el rol no alcanza |
| CORS bloqueado en el navegador | `APP_CORS_ALLOWED_ORIGIN` no coincide exactamente con el origen |
| `Access denied` de MySQL | Usuario, clave o base equivocados en el archivo de entorno |
| React da 404 al recargar una ruta | Falta el bloque `try_files` de Nginx |

## 14. Actualizar una versión desplegada

Reemplazo solo el JAR del microservicio que cambió y reinicio su unidad. El otro sigue
atendiendo sin interrupción.

```bash
sudo systemctl stop nutrivida-auth
sudo mv /tmp/nutrivida-auth.jar /opt/nutrivida-auth/app.jar
sudo chown ubuntu:ubuntu /opt/nutrivida-auth/app.jar
sudo systemctl start nutrivida-auth
sudo systemctl status nutrivida-auth --no-pager
```

Para el frontend recompilo y copio otra vez `dist`, sin reiniciar los backend.
