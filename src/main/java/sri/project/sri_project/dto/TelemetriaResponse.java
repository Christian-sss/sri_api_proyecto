package sri.project.sri_project.dto;

public record TelemetriaResponse(
        String fechaLectura,
        String etiqueta,
        Integer humedad,
        Double distancia
) {
}
