package sri.project.sri_project.dto;

public record DistribucionModosResponse(
        long manual,
        long automatico,
        long total,
        String cultivoActivo
) {
}
