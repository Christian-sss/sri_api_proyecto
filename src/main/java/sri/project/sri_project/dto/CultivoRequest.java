package sri.project.sri_project.dto;

public record CultivoRequest(
        String nombre,
        Integer humedadMinOptima,
        Integer humedadMaxOptima,
        Integer duracionRiegoMinutos,
        String tratoRecomendado
) {
}
