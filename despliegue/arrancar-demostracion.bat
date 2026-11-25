@echo off
chcp 65001 >nul
REM ---------------------------------------------------------------------------
REM Arranque completo del sistema para la demostracion.
REM
REM Lo escribi porque el dia de la presentacion no quiero depender de recordar
REM cuatro comandos en cuatro terminales distintas. Este archivo levanta, en
REM orden, la base de datos, los dos microservicios y el servidor que entrega la
REM pagina, y al final me muestra la direccion que tengo que abrir.
REM
REM Supone que los dos repositorios estan uno al lado del otro dentro de
REM C:\DuocTrabajos. Si los muevo, cambio la variable BASE de aqui abajo.
REM ---------------------------------------------------------------------------

setlocal enabledelayedexpansion

set "BASE=C:\DuocTrabajos"
set "JAVA=C:\ProgramacionApp\Java\jdk-21\bin\java.exe"
set "MYSQLD=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe"
set "MYSQLINI=C:\DuocTrabajos\mysql-datos\my.ini"
set "LOGS=C:\DuocTrabajos\mysql-datos"

echo.
echo ===========================================================
echo   Clinica NutriVida - arranque del sistema
echo ===========================================================
echo.

REM --- 1. Base de datos -------------------------------------------------------
REM Reviso si el puerto 3306 ya esta ocupado antes de lanzar otra instancia:
REM arrancar dos veces el mismo motor sobre la misma carpeta de datos la corrompe.
netstat -ano | findstr /r /c:":3306 .*LISTENING" >nul
if %errorlevel%==0 (
    echo [1/4] MySQL ya estaba corriendo.
) else (
    echo [1/4] Arrancando MySQL...
    start "" /b "%MYSQLD%" --defaults-file="%MYSQLINI%"
)

REM --- Clave de firma de los tokens -------------------------------------------
REM Los dos microservicios tienen que firmar y verificar con la misma clave, y esa
REM clave no puede estar escrita en el repositorio: si estuviera, cualquiera que
REM viera el codigo podria fabricar tokens validos y entrar como administrador.
REM
REM Por eso la genero aqui, al azar, la primera vez que arranco el sistema y la
REM guardo fuera de los dos proyectos. Las veces siguientes reutilizo la misma, asi
REM los tokens que ya emiti siguen siendo validos. Si quiero rotarla, borro el
REM archivo y la proxima ejecucion crea una nueva.
set "CLAVEJWT=%LOGS%\jwt-demostracion.key"
if not exist "%CLAVEJWT%" (
    echo       Generando la clave de firma de los tokens...
    powershell -NoProfile -Command "$bytes = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Fill($bytes); [Convert]::ToBase64String($bytes) | Set-Content -NoNewline -Path '%CLAVEJWT%' -Encoding ascii"
)
for /f "usebackq delims=" %%k in ("%CLAVEJWT%") do set "JWT_SECRET=%%k"

if not defined JWT_SECRET (
    echo   ERROR: no pude leer la clave en %CLAVEJWT%
    echo   Sin esa clave los microservicios no arrancan, que es justo lo que quiero.
    pause
    exit /b 1
)

REM --- 2. Microservicio de autenticacion --------------------------------------
netstat -ano | findstr /r /c:":8081 .*LISTENING" >nul
if %errorlevel%==0 (
    echo [2/4] El servicio de autenticacion ya estaba corriendo.
) else (
    echo [2/4] Arrancando nutrivida-auth en el puerto 8081...
    start "" /b "%JAVA%" -jar "%BASE%\BackendNutriVida\nutrivida-auth\target\nutrivida-auth-0.0.1-SNAPSHOT.jar" > "%LOGS%\auth-salida.log" 2>&1
)

REM --- 3. Microservicio de citas ----------------------------------------------
netstat -ano | findstr /r /c:":8082 .*LISTENING" >nul
if %errorlevel%==0 (
    echo [3/4] El servicio de citas ya estaba corriendo.
) else (
    echo [3/4] Arrancando nutrivida-citas en el puerto 8082...
    start "" /b "%JAVA%" -jar "%BASE%\BackendNutriVida\nutrivida-citas\target\nutrivida-citas-0.0.1-SNAPSHOT.jar" > "%LOGS%\citas-salida.log" 2>&1
)

REM --- Espero a que los dos backends respondan --------------------------------
REM Spring Boot tarda unos segundos en levantar. Si abriera la pagina antes de
REM que esten listos, el navegador mostraria errores de conexion y parecaria que
REM el sistema no funciona.
echo.
echo       Esperando a que los microservicios esten listos...
set /a intentos=0
:esperar
set /a intentos+=1
curl -s -o nul http://127.0.0.1:8081/v3/api-docs
if errorlevel 1 goto seguir_esperando
curl -s -o nul http://127.0.0.1:8082/v3/api-docs
if errorlevel 1 goto seguir_esperando
goto listos
:seguir_esperando
if %intentos% geq 60 (
    echo       No respondieron a tiempo. Revisa %LOGS%\auth-error.log y %LOGS%\citas-error.log
    goto listos
)
REM Espero dos segundos. Uso ping en vez de timeout porque timeout falla cuando
REM el script se ejecuta sin una consola interactiva, y ahi ensucia la salida.
ping -n 3 127.0.0.1 >nul
goto esperar
:listos
echo       Microservicios respondiendo.

REM --- 4. Pagina web ----------------------------------------------------------
REM Este servidor entrega la aplicacion React ya compilada y ademas reparte las
REM llamadas a cada microservicio, igual que hace Nginx en un servidor.
netstat -ano | findstr /r /c:":4173 .*LISTENING" >nul
if %errorlevel%==0 (
    echo [4/4] La pagina ya estaba publicada.
) else (
    echo [4/4] Publicando la pagina en el puerto 4173...
    start "" /b cmd /c "cd /d %BASE%\ClinicaNutriVidaEP2 && npm run preview > %LOGS%\preview.log 2>&1"
    ping -n 6 127.0.0.1 >nul
)

REM --- Direcciones para abrir -------------------------------------------------
echo.
echo ===========================================================
echo   Direcciones para la demostracion
echo ===========================================================
echo.
echo   En este mismo equipo:
echo       http://localhost:4173
echo.
echo   Desde otro computador de la misma red:
for /f "tokens=2 delims=:" %%i in ('ipconfig ^| findstr /c:"IPv4"') do (
    set "ip=%%i"
    set "ip=!ip: =!"
    echo       http://!ip!:4173
)
echo.
echo   Swagger de los microservicios:
echo       http://localhost:8081/swagger-ui/index.html
echo       http://localhost:8082/swagger-ui/index.html
echo.
echo   Si la red de la sala no deja que un equipo vea a otro,
echo   levanto el tunel para obtener una direccion publica.
echo.
echo ===========================================================
echo.
pause
