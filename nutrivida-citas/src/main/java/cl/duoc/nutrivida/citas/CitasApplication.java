package cl.duoc.nutrivida.citas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Esta clase es el punto de entrada del microservicio de citas y fichas clínicas.
@SpringBootApplication
public class CitasApplication {

    public static void main(String[] args) {
        // Levanto el contenedor de Spring, que abre el servidor web en el puerto 8082.
        SpringApplication.run(CitasApplication.class, args);
    }
}
