-- Script de preparación de la base de datos de la Clínica NutriVida.
-- Lo ejecuto con un usuario administrador de MySQL, antes de levantar los microservicios.
-- Sigo la Guía 15: una base por microservicio y una cuenta propia para cada aplicación.

-- Creo las dos bases por separado, porque cada microservicio es dueño de sus propias tablas
-- y ninguno debe poder leer ni escribir en las del otro.
-- Uso utf8mb4 para poder guardar tildes y la ñ sin perder caracteres.
CREATE DATABASE IF NOT EXISTS nutrivida_auth
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS nutrivida_citas
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Creo una cuenta por aplicación para no ejecutar nada con root.
-- Reemplazo CAMBIAR_CLAVE_AUTH y CAMBIAR_CLAVE_CITAS por claves reales, que nunca van a Git:
-- viajan por variables de entorno, como indica la Guía 18.
CREATE USER IF NOT EXISTS 'nutrivida_auth_app'@'localhost'
  IDENTIFIED BY 'CAMBIAR_CLAVE_AUTH';

CREATE USER IF NOT EXISTS 'nutrivida_citas_app'@'localhost'
  IDENTIFIED BY 'CAMBIAR_CLAVE_CITAS';

-- Cada cuenta recibe permisos solo sobre su propia base. Así, si una se filtrara,
-- no podría tocar los datos del otro microservicio.
GRANT ALL PRIVILEGES ON nutrivida_auth.*
  TO 'nutrivida_auth_app'@'localhost';

GRANT ALL PRIVILEGES ON nutrivida_citas.*
  TO 'nutrivida_citas_app'@'localhost';

-- Limito las cuentas a localhost: nadie puede conectarse a MySQL desde fuera del servidor.
FLUSH PRIVILEGES;

-- Comprobación: estas dos consultas me confirman que quedó todo creado.
SHOW DATABASES LIKE 'nutrivida%';
SELECT user, host FROM mysql.user WHERE user LIKE 'nutrivida%';

-- Las tablas no las creo a mano: JPA las genera al arrancar cada microservicio
-- con spring.jpa.hibernate.ddl-auto=update, y los datos iniciales del catálogo
-- los carga la clase DatosIniciales de cada proyecto.
