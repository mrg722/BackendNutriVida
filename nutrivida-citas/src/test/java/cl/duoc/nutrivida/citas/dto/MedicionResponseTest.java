package cl.duoc.nutrivida.citas.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cl.duoc.nutrivida.citas.model.Medicion;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// El IMC no está guardado en la base: es un dato derivado y lo calculo al responder.
// Estas pruebas comprueban ese cálculo y que no se caiga con datos incompletos.
class MedicionResponseTest {

    private Medicion medicion(Double pesoKg, Double tallaCm) {
        Medicion medicion = new Medicion();
        medicion.setId(1L);
        medicion.setCorreoPaciente("paciente@nutrivida.cl");
        medicion.setCorreoNutricionista("carolina.fuentes@nutrivida.cl");
        medicion.setFecha(LocalDate.now());
        medicion.setPesoKg(pesoKg);
        medicion.setTallaCm(tallaCm);
        return medicion;
    }

    @Test
    @DisplayName("Calculo el IMC con el peso en kilos y la talla en metros al cuadrado")
    void calculoDelImc() {
        // 82,5 / (1,72 * 1,72) = 27,88..., que informo como 27.9
        assertEquals(27.9, MedicionResponse.desde(medicion(82.5, 172.0)).imc());
    }

    @Test
    @DisplayName("Redondeo el IMC a un decimal")
    void redondeoAUnDecimal() {
        // 70 / (1,75 * 1,75) = 22,857..., que informo como 22.9
        assertEquals(22.9, MedicionResponse.desde(medicion(70.0, 175.0)).imc());
    }

    @Test
    @DisplayName("Si falta la talla no calculo el IMC en vez de fallar")
    void sinTallaNoHayImc() {
        assertNull(MedicionResponse.desde(medicion(82.5, null)).imc());
    }

    @Test
    @DisplayName("Una talla en cero no provoca una división por cero")
    void tallaEnCero() {
        assertNull(MedicionResponse.desde(medicion(82.5, 0.0)).imc());
    }

    @Test
    @DisplayName("Copio el resto de los datos tal como vienen de la entidad")
    void copiaDeLosDemasCampos() {
        Medicion medicion = medicion(82.5, 172.0);
        medicion.setCinturaCm(95.0);
        medicion.setObservaciones("Primer control");
        medicion.setPlanAlimenticio("Cuatro comidas diarias");

        MedicionResponse respuesta = MedicionResponse.desde(medicion);

        assertEquals("paciente@nutrivida.cl", respuesta.correoPaciente());
        assertEquals("carolina.fuentes@nutrivida.cl", respuesta.correoNutricionista());
        assertEquals(95.0, respuesta.cinturaCm());
        assertEquals("Primer control", respuesta.observaciones());
        assertEquals("Cuatro comidas diarias", respuesta.planAlimenticio());
    }
}
