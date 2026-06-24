package sri.project.sri_project.dto;

public record AlertaRiegoResponse(
        Long id,
        String tipo,
        String titulo,
        String mensaje,
        String fecha
) {
}
