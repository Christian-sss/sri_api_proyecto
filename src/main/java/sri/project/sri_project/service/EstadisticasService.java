package sri.project.sri_project.service;

import sri.project.sri_project.dto.DistribucionModosResponse;
import sri.project.sri_project.dto.ConsumoAguaDetalleResponse;
import sri.project.sri_project.dto.EstadisticasResumenResponse;
import sri.project.sri_project.dto.TelemetriaResponse;

import java.util.List;
import java.util.Map;

public interface EstadisticasService {

    Map<String, Integer> obtenerDatosHumedadHistorica();
    Map<String, Integer> obtenerDatosModosRiego();
    EstadisticasResumenResponse obtenerResumen();
    List<TelemetriaResponse> obtenerTelemetriaReciente();
    List<TelemetriaResponse> obtenerTelemetriaReciente(Integer cultivoId, boolean soloMantenimiento);
    DistribucionModosResponse obtenerDistribucionModosMesActual();
    DistribucionModosResponse obtenerDistribucionModosMesActual(Integer cultivoId, boolean soloMantenimiento);
    List<ConsumoAguaDetalleResponse> obtenerConsumoAguaDetalle(Integer cultivoId, boolean soloMantenimiento);

}
