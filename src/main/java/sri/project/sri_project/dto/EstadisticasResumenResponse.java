package sri.project.sri_project.dto;

import java.util.List;

public record EstadisticasResumenResponse(
        long totalRiegosMes,
        long riegosManualesMes,
        long riegosAutomaticosMes,
        long riegosCompletados,
        double promedioHumedadGanada,
        String ultimoRiego,
        List<String> labelsDuracion,
        List<Long> datosDuracionSegundos
) {
}
