package sri.project.sri_project.dto;

public record CultivoResponse(
        Integer id,
        String nombre,
        Integer humedadMinOptima,
        Integer humedadMaxOptima,
        Integer duracionRiegoMinutos,
        String tratoRecomendado,
        Boolean activo
) {
}
