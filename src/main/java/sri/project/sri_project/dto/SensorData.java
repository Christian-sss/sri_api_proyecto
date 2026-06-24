package sri.project.sri_project.dto;

public record SensorData(
        int humedad,
        double distancia,
        Boolean bombaActiva
) {
    public SensorData(int humedad, double distancia) {
        this(humedad, distancia, null);
    }
}
