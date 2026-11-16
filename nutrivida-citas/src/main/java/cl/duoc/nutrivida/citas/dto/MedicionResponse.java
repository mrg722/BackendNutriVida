package cl.duoc.nutrivida.citas.dto;

import cl.duoc.nutrivida.citas.model.Medicion;
import java.time.LocalDate;

// Esta es la vista de una medición. Incluyo el IMC aunque no exista como columna, porque es
// un dato derivado: lo calculo al responder para no guardar información repetida en la base.
public record MedicionResponse(
        Long id,
        String correoPaciente,
        String correoNutricionista,
        LocalDate fecha,
        Double pesoKg,
        Double tallaCm,
        Double cinturaCm,
        Double imc,
        String observaciones,
        String planAlimenticio) {

    public static MedicionResponse desde(Medicion medicion) {
        return new MedicionResponse(
                medicion.getId(),
                medicion.getCorreoPaciente(),
                medicion.getCorreoNutricionista(),
                medicion.getFecha(),
                medicion.getPesoKg(),
                medicion.getTallaCm(),
                medicion.getCinturaCm(),
                calcularImc(medicion.getPesoKg(), medicion.getTallaCm()),
                medicion.getObservaciones(),
                medicion.getPlanAlimenticio());
    }

    // IMC = peso en kilos dividido por la talla en metros al cuadrado.
    private static Double calcularImc(Double pesoKg, Double tallaCm) {
        // Me protejo de una talla nula o cero para no dividir por cero.
        if (pesoKg == null || tallaCm == null || tallaCm <= 0) {
            return null;
        }
        double tallaMetros = tallaCm / 100.0;
        double imc = pesoKg / (tallaMetros * tallaMetros);
        // Redondeo a un decimal, que es como se informa el IMC en la práctica clínica.
        return Math.round(imc * 10.0) / 10.0;
    }
}
