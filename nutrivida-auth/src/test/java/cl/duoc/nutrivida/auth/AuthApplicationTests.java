package cl.duoc.nutrivida.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Con esta prueba compruebo que el contexto de Spring arranca y que no hay beans mal declarados.
@SpringBootTest
class AuthApplicationTests {

    @Test
    void contextLoads() {
        // No necesito aserciones: si el contexto no carga, la prueba falla por sí sola.
    }
}
