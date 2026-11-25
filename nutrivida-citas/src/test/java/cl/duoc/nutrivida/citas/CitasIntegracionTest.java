package cl.duoc.nutrivida.citas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// Pruebas de integración del microservicio de citas. Levanto la aplicación completa y hago
// peticiones HTTP reales, de modo que cada una atraviesa el filtro JWT y las reglas de
// SecurityConfig, igual que una petición del frontend.
//
// Este microservicio no emite tokens, solo los verifica. Así que en la prueba firmo el token yo
// mismo con la misma clave y el mismo emisor que tiene configurados, que es exactamente lo que
// hace nutrivida-auth en producción. Eso me permite probar cada rol sin depender del otro
// microservicio, que es justamente la independencia que buscábamos en la arquitectura.
@SpringBootTest
@AutoConfigureMockMvc
// Las pruebas necesitan una clave de firma, y la aplicación ya no trae ninguna por defecto.
// Le paso aquí una fija y exclusiva de las pruebas: así el conjunto corre solo, sin depender de
// la variable de entorno del equipo, y la clave real de la demostración nunca entra al código.
@TestPropertySource(properties = "app.jwt.secret=Y2xhdmUtZXhjbHVzaXZhLXBhcmEtbGFzLXBydWViYXMtYXV0b21hdGl6YWRhcy1kZS1udXRyaXZpZGE=")
class CitasIntegracionTest {

    @Autowired
    private MockMvc mockMvc;


    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer}")
    private String issuer;

    // Firmo un token con el rol que necesite para cada prueba.
    private String token(String correo, String rol) {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Date ahora = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(correo)
                .claim("rol", rol)
                .claim("nombre", "Usuario de prueba")
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + 3600000))
                .signWith(clave)
                .compact();
    }

    private String tokenAdmin() {
        return token("admin@nutrivida.cl", "ADMIN");
    }

    private String tokenPaciente() {
        return token("paciente@nutrivida.cl", "PACIENTE");
    }

    @Test
    @DisplayName("El catálogo sin token responde 401")
    void catalogoSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/servicios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El catálogo con cualquier sesión válida responde 200 y trae los servicios del caso")
    void catalogoConSesion() throws Exception {
        mockMvc.perform(get("/api/v1/servicios").param("soloActivos", "true")
                        .header("Authorization", "Bearer " + tokenPaciente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").isNotEmpty());
    }

    @Test
    @DisplayName("Crear un servicio con rol insuficiente responde 403, no 401")
    void crearServicioComoPaciente() throws Exception {
        mockMvc.perform(post("/api/v1/servicios")
                        .header("Authorization", "Bearer " + tokenPaciente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioDePrueba("XX900")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El ciclo completo del CRUD como administrador: 201, 200 y 204")
    void cicloCrudComoAdministrador() throws Exception {
        String token = tokenAdmin();

        // Creo el servicio y me quedo con el id que asignó la base de datos.
        String creado = mockMvc.perform(post("/api/v1/servicios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioDePrueba("XX901")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        long id = extraerId(creado);

        // Lo leo de vuelta para comprobar que quedó guardado de verdad.
        mockMvc.perform(get("/api/v1/servicios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("XX901"));

        // Repetir el código choca con la regla de unicidad y responde 409.
        mockMvc.perform(post("/api/v1/servicios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioDePrueba("XX901")))
                .andExpect(status().isConflict());

        // Lo elimino y compruebo que DELETE devuelve 204 sin cuerpo.
        mockMvc.perform(delete("/api/v1/servicios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Y que después ya no existe.
        mockMvc.perform(get("/api/v1/servicios/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un cuerpo que no cumple las validaciones responde 400 y no se guarda")
    void servicioConDatosInvalidos() throws Exception {
        mockMvc.perform(post("/api/v1/servicios")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"XX902\",\"tipo\":\"Consulta\",\"nombre\":\"Prueba\","
                                + "\"modalidad\":\"Presencial\",\"precio\":-1,"
                                + "\"descripcion\":\"Precio negativo\",\"activo\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("Un servicio inexistente responde 404 con el error en JSON")
    void servicioInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/servicios/999999").header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("El listado completo de citas es solo del administrador")
    void listadoCompletoDeCitas() throws Exception {
        // Con rol de paciente la clínica entera no se ve: responde 403.
        mockMvc.perform(get("/api/v1/citas").header("Authorization", "Bearer " + tokenPaciente()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/citas").header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Mis citas funciona para cualquier sesión válida")
    void misCitas() throws Exception {
        mockMvc.perform(get("/api/v1/citas/mias").header("Authorization", "Bearer " + tokenPaciente()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una hora fuera del horario de atención responde 400")
    void horaFueraDeHorario() throws Exception {
        // El 2030-12-13 es viernes, uno de los días que atiende esta nutricionista, así que lo
        // único que está mal aquí es la hora: ella cierra a las 17:00.
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + tokenPaciente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nutricionistaId\":1,\"servicioId\":1,"
                                + "\"fecha\":\"2030-12-13\",\"hora\":\"20:00\","
                                + "\"motivo\":\"Consulta fuera del horario de atencion\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Agendar en un día que la nutricionista no atiende responde 400")
    void diaQueNoAtiende() throws Exception {
        // Esta nutricionista atiende lunes, miércoles y viernes, y el 2030-12-15 es domingo.
        // La hora sí cae dentro de su jornada: lo que no corresponde es el día. Antes esta
        // petición se aceptaba, porque la regla usaba un horario único para toda la clínica y
        // nunca miraba los días que la tabla guarda para cada profesional.
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + tokenPaciente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nutricionistaId\":1,\"servicioId\":1,"
                                + "\"fecha\":\"2030-12-15\",\"hora\":\"10:00\","
                                + "\"motivo\":\"Consulta en un dia que no corresponde\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Dos citas que se cruzan por su duración responden 409")
    void citasQueSeCruzan() throws Exception {
        // La primera consulta dura 50 minutos, así que una cita a las 10:00 tiene ocupada la
        // agenda hasta las 10:50. Después pido un control de 30 minutos a las 10:30: empieza a
        // otra hora, pero cae justo dentro de la primera.
        //
        // Esta es la prueba de la regla que corregí: antes solo comparaba la hora de inicio, de
        // modo que las dos citas convivían y la nutricionista quedaba con dos pacientes encima.
        String primera = mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + tokenPaciente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nutricionistaId\":1,\"servicioId\":1,"
                                + "\"fecha\":\"2030-12-13\",\"hora\":\"10:00\","
                                + "\"motivo\":\"Primera consulta de la prueba de traslape\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + tokenPaciente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nutricionistaId\":1,\"servicioId\":2,"
                                + "\"fecha\":\"2030-12-13\",\"hora\":\"10:30\","
                                + "\"motivo\":\"Control que se encima con la consulta anterior\"}"))
                .andExpect(status().isConflict());

        // Borro la cita que creé: la prueba no tiene por qué dejar horas inventadas en la
        // agenda real de la clínica. De paso esto ejercita el DELETE del administrador.
        mockMvc.perform(delete("/api/v1/citas/" + extraerId(primera))
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Un motivo demasiado corto lo rechaza la validación con 400")
    void motivoDemasiadoCorto() throws Exception {
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + tokenPaciente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nutricionistaId\":1,\"servicioId\":1,"
                                + "\"fecha\":\"2030-12-13\",\"hora\":\"10:00\",\"motivo\":\"corto\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Escribir en la ficha clínica con rol de paciente responde 403")
    void registrarMedicionComoPaciente() throws Exception {
        mockMvc.perform(post("/api/v1/mediciones")
                        .header("Authorization", "Bearer " + tokenPaciente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoPaciente\":\"paciente@nutrivida.cl\","
                                + "\"fecha\":\"2026-11-17\",\"pesoKg\":80,\"tallaCm\":170}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Las estadísticas son solo del administrador")
    void estadisticas() throws Exception {
        mockMvc.perform(get("/api/v1/estadisticas").header("Authorization", "Bearer " + tokenPaciente()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/estadisticas").header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCitas").isNumber())
                .andExpect(jsonPath("$.tasaInasistencia").isNumber())
                .andExpect(jsonPath("$.citasPorEstado").isMap());
    }

    @Test
    @DisplayName("Un token firmado con otra clave responde 401")
    void tokenDeOtraClave() throws Exception {
        // Firmo con una clave distinta: la firma no corresponde y el filtro descarta la identidad.
        SecretKey otraClave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLXByb2Jhci1lbC1yZWNoYXpvLWRlLWZpcm1h"));
        Date ahora = new Date();
        String tokenAjeno = Jwts.builder()
                .issuer(issuer)
                .subject("admin@nutrivida.cl")
                .claim("rol", "ADMIN")
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + 3600000))
                .signWith(otraClave)
                .compact();

        mockMvc.perform(get("/api/v1/servicios").header("Authorization", "Bearer " + tokenAjeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token vencido responde 401 aunque la firma sea correcta")
    void tokenVencido() throws Exception {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Date hace2Horas = new Date(System.currentTimeMillis() - 7200000);
        String vencido = Jwts.builder()
                .issuer(issuer)
                .subject("admin@nutrivida.cl")
                .claim("rol", "ADMIN")
                .issuedAt(hace2Horas)
                // Venció hace una hora.
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(clave)
                .compact();

        mockMvc.perform(get("/api/v1/servicios").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    // Cuerpo reutilizable para crear servicios de prueba, con el código como único dato variable.
    private String servicioDePrueba(String codigo) {
        return "{\"codigo\":\"" + codigo + "\",\"tipo\":\"Consulta\","
                + "\"nombre\":\"Servicio de prueba de integracion\",\"duracionMinutos\":30,"
                + "\"modalidad\":\"Presencial\",\"precio\":15000,"
                + "\"descripcion\":\"Creado por las pruebas de integracion\",\"activo\":true}";
    }

    // Saco el id que asignó la base de datos del JSON de la respuesta, sin usar un mapeador.
    private long extraerId(String json) {
        java.util.regex.Matcher encontrado = java.util.regex.Pattern
                .compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        return encontrado.find() ? Long.parseLong(encontrado.group(1)) : -1L;
    }
}
