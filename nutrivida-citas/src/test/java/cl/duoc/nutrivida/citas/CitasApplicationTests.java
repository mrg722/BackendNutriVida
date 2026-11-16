package cl.duoc.nutrivida.citas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Con esta prueba compruebo que el contexto arranca, que la conexión a MySQL funciona
// y que todas las entidades, repositorios y controladores quedaron bien declarados.
@SpringBootTest
class CitasApplicationTests {

    @Test
    void contextLoads() {
        // Si algún bean estuviera mal configurado, el contexto no cargaría y esto fallaría.
    }
}
