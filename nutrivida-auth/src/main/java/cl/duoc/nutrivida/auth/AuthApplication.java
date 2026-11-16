package cl.duoc.nutrivida.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Esta clase es el punto de entrada del microservicio de autenticación.
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        // Levanto el contenedor de Spring, que a su vez abre el servidor web en el puerto 8081.
        SpringApplication.run(AuthApplication.class, args);
    }
}
