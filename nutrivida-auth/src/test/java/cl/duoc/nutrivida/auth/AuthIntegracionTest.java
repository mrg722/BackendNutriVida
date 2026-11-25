package cl.duoc.nutrivida.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// Estas son las pruebas de integración del microservicio de autenticación.
//
// Se diferencian de las unitarias en algo importante: las unitarias reemplazan el repositorio y
// el emisor de tokens por dobles, así que comprueban la regla de negocio pero nunca tocan HTTP.
// Estas levantan la aplicación completa y hacen peticiones reales contra los endpoints, así que
// atraviesan la cadena de seguridad entera: el filtro JWT, las reglas de SecurityConfig y el
// manejador de errores. Es la única forma de comprobar automáticamente que un rol insuficiente
// responde 403 y no 401, que es justo la distinción que hay que demostrar.
@SpringBootTest
@AutoConfigureMockMvc
// Las pruebas necesitan una clave de firma, y la aplicación ya no trae ninguna por defecto.
// Le paso aquí una fija y exclusiva de las pruebas: así el conjunto corre solo, sin depender de
// la variable de entorno del equipo, y la clave real de la demostración nunca entra al código.
@TestPropertySource(properties = "app.jwt.secret=Y2xhdmUtZXhjbHVzaXZhLXBhcmEtbGFzLXBydWViYXMtYXV0b21hdGl6YWRhcy1kZS1udXRyaXZpZGE=")
class AuthIntegracionTest {

    // MockMvc me deja hacer peticiones HTTP sin abrir un puerto real.
    @Autowired
    private MockMvc mockMvc;


    private static final String CLAVE_DEMOSTRACION = "Cambiar123!";

    // Inicio sesión de verdad y devuelvo el token, para usarlo en las pruebas que lo necesitan.
    private String tokenDe(String correo) throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(correo, CLAVE_DEMOSTRACION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return extraerTexto(respuesta, "token");
    }

    private String cuerpoLogin(String correo, String password) {
        return "{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    @DisplayName("El login con credenciales correctas responde 200 y devuelve el token y el rol")
    void loginCorrecto() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("admin@nutrivida.cl", CLAVE_DEMOSTRACION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.correo").value("admin@nutrivida.cl"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                // Compruebo que la respuesta nunca incluye el hash de la contraseña.
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("El login con la contraseña equivocada responde 401 con el error en JSON")
    void loginConClaveIncorrecta() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("admin@nutrivida.cl", "clave-que-no-es")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Un correo mal formado lo rechaza la validación con 400, antes de tocar la base")
    void loginConCorreoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("esto-no-es-un-correo", "Clave123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("El perfil sin token responde 401")
    void perfilSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El perfil con un token válido responde 200 con los datos de la sesión")
    void perfilConToken() throws Exception {
        String token = tokenDe("paciente@nutrivida.cl");

        mockMvc.perform(get("/api/v1/auth/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("paciente@nutrivida.cl"))
                .andExpect(jsonPath("$.rol").value("PACIENTE"));
    }

    @Test
    @DisplayName("Un token alterado responde 401 aunque la ruta exija solo estar autenticado")
    void tokenAlterado() throws Exception {
        String token = tokenDe("paciente@nutrivida.cl");

        mockMvc.perform(get("/api/v1/auth/perfil").header("Authorization", "Bearer " + token + "xx"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La renovación entrega un token nuevo y distinto del anterior")
    void renovacionDelToken() throws Exception {
        String token = tokenDe("paciente@nutrivida.cl");

        String respuesta = mockMvc.perform(post("/api/v1/auth/renovar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String tokenNuevo = extraerTexto(respuesta, "token");
        // El token renovado tiene que servir para seguir trabajando.
        mockMvc.perform(get("/api/v1/auth/perfil").header("Authorization", "Bearer " + tokenNuevo))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("La gestión de usuarios sin token responde 401")
    void usuariosSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La gestión de usuarios con rol ADMIN responde 200")
    void usuariosComoAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + tokenDe("admin@nutrivida.cl")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").isNotEmpty())
                // Ninguna respuesta del listado puede exponer el hash de la contraseña.
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @DisplayName("La gestión de usuarios con rol insuficiente responde 403, no 401")
    void usuariosComoPaciente() throws Exception {
        // Esta es la distinción que pide la guía: el token es válido, pero el rol no alcanza.
        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenDe("paciente@nutrivida.cl")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un nutricionista tampoco puede entrar a la gestión de usuarios")
    void usuariosComoNutricionista() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + tokenDe("carolina.fuentes@nutrivida.cl")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Una ruta que no declaré queda cerrada")
    void rutaNoDeclarada() throws Exception {
        mockMvc.perform(get("/api/v1/inventada")
                        .header("Authorization", "Bearer " + tokenDe("admin@nutrivida.cl")))
                .andExpect(status().isForbidden());
    }

    // Leo un campo de texto del JSON con una expresión regular. No uso un mapeador porque lo
    // único que necesito de la respuesta es el token, y así la prueba no depende de que haya
    // un bean de serialización publicado en el contexto.
    private String extraerTexto(String json, String campo) {
        java.util.regex.Matcher encontrado = java.util.regex.Pattern
                .compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return encontrado.find() ? encontrado.group(1) : null;
    }
}
