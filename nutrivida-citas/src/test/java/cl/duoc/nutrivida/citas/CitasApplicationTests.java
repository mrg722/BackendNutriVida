package cl.duoc.nutrivida.citas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Con esta prueba compruebo que el contexto arranca, que la conexión a MySQL funciona
// y que todas las entidades, repositorios y controladores quedaron bien declarados.
@SpringBootTest
// Igual que en las pruebas de integración: la clave de firma ya no viene por defecto en la
// aplicación, así que aquí le doy una fija de pruebas para que el contexto pueda levantar.
@TestPropertySource(properties = "app.jwt.secret=Y2xhdmUtZXhjbHVzaXZhLXBhcmEtbGFzLXBydWViYXMtYXV0b21hdGl6YWRhcy1kZS1udXRyaXZpZGE=")
class CitasApplicationTests {

    @Test
    void contextLoads() {
        // Si algún bean estuviera mal configurado, el contexto no cargaría y esto fallaría.
    }
}
