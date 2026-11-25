package cl.duoc.nutrivida.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Con esta prueba compruebo que el contexto de Spring arranca y que no hay beans mal declarados.
@SpringBootTest
// Igual que en las pruebas de integración: la clave de firma ya no viene por defecto en la
// aplicación, así que aquí le doy una fija de pruebas para que el contexto pueda levantar.
@TestPropertySource(properties = "app.jwt.secret=Y2xhdmUtZXhjbHVzaXZhLXBhcmEtbGFzLXBydWViYXMtYXV0b21hdGl6YWRhcy1kZS1udXRyaXZpZGE=")
class AuthApplicationTests {

    @Test
    void contextLoads() {
        // No necesito aserciones: si el contexto no carga, la prueba falla por sí sola.
    }
}
