package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sri.project.sri_project.dto.DistribucionModosResponse;
import sri.project.sri_project.dto.ConsumoAguaDetalleResponse;
import sri.project.sri_project.dto.EstadisticasResumenResponse;
import sri.project.sri_project.dto.TelemetriaResponse;
import sri.project.sri_project.service.EstadisticasService;

import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasApiController {

    private final EstadisticasService estadisticasService;

    @GetMapping("/resumen")
    public EstadisticasResumenResponse obtenerResumen() {
        return estadisticasService.obtenerResumen();
    }

    @GetMapping("/telemetria")
    public List<TelemetriaResponse> obtenerTelemetria(@RequestParam(required = false, name = "cultivoId") String cultivoId) {
        FiltroCultivo filtro = resolverFiltroCultivo(cultivoId);
        return estadisticasService.obtenerTelemetriaReciente(filtro.cultivoId(), filtro.soloMantenimiento());
    }

    @GetMapping("/distribucion-modos")
    public DistribucionModosResponse obtenerDistribucionModos(@RequestParam(required = false, name = "cultivoId") String cultivoId) {
        FiltroCultivo filtro = resolverFiltroCultivo(cultivoId);
        return estadisticasService.obtenerDistribucionModosMesActual(filtro.cultivoId(), filtro.soloMantenimiento());
    }

    @GetMapping("/consumo-detalle")
    public List<ConsumoAguaDetalleResponse> obtenerConsumoAguaDetalle(
            @RequestParam(required = false, name = "cultivoId") String cultivoId) {
        FiltroCultivo filtro = resolverFiltroCultivo(cultivoId);
        return estadisticasService.obtenerConsumoAguaDetalle(filtro.cultivoId(), filtro.soloMantenimiento());
    }

    private FiltroCultivo resolverFiltroCultivo(String cultivoId) {
        if (cultivoId == null || cultivoId.isBlank()) {
            return new FiltroCultivo(null, false);
        }

        if ("null_value".equalsIgnoreCase(cultivoId.trim())) {
            return new FiltroCultivo(null, true);
        }

        try {
            return new FiltroCultivo(Integer.valueOf(cultivoId), false);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cultivoId debe ser numerico o null_value.");
        }
    }

    private record FiltroCultivo(Integer cultivoId, boolean soloMantenimiento) {
    }
}
