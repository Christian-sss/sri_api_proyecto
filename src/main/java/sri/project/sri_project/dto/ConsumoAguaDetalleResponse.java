package sri.project.sri_project.dto;

public record ConsumoAguaDetalleResponse(
        String fecha,
        String cultivo,
        String horaInicio,
        String horaFin,
        double litrosConsumidos
) {
}
