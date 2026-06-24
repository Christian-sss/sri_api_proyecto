package sri.project.sri_project.service.serviceImpl;

import org.springframework.stereotype.Service;
import sri.project.sri_project.dto.DistribucionModosResponse;
import sri.project.sri_project.dto.ConsumoAguaDetalleResponse;
import sri.project.sri_project.dto.EstadisticasResumenResponse;
import sri.project.sri_project.dto.TelemetriaResponse;
import sri.project.sri_project.model.ConfiguracionRiego;
import sri.project.sri_project.model.Cultivo;
import sri.project.sri_project.model.EventoRiego;
import sri.project.sri_project.model.LecturaSensor;
import sri.project.sri_project.model.enums.EstadoRiego;
import sri.project.sri_project.model.enums.ModoRiego;
import sri.project.sri_project.repository.ConfiguracionRiegoRepository;
import sri.project.sri_project.repository.EventoRiegoRepository;
import sri.project.sri_project.repository.LecturaSensorRepository;
import sri.project.sri_project.repository.PerfilCultivoRepository;
import sri.project.sri_project.service.EstadisticasService;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisualizarEstadisticasServiceImpl implements EstadisticasService {

    private final EventoRiegoRepository eventoRiegoRepository;
    private final LecturaSensorRepository lecturaSensorRepository;
    private final ConfiguracionRiegoRepository configuracionRiegoRepository;
    private final PerfilCultivoRepository perfilCultivoRepository;

    public VisualizarEstadisticasServiceImpl(
            EventoRiegoRepository eventoRiegoRepository,
            LecturaSensorRepository lecturaSensorRepository,
            ConfiguracionRiegoRepository configuracionRiegoRepository,
            PerfilCultivoRepository perfilCultivoRepository
    ) {
        this.eventoRiegoRepository = eventoRiegoRepository;
        this.lecturaSensorRepository = lecturaSensorRepository;
        this.configuracionRiegoRepository = configuracionRiegoRepository;
        this.perfilCultivoRepository = perfilCultivoRepository;
    }

    @Override
    public Map<String, Integer> obtenerDatosHumedadHistorica() {
        Map<String, Integer> datos = new LinkedHashMap<>();
        datos.put("06:00", 38);
        datos.put("09:00", 55);
        datos.put("12:00", 73);
        datos.put("15:00", 90);
        return datos;
    }

    @Override
    public Map<String, Integer> obtenerDatosModosRiego() {
        Map<String, Integer> datos = new LinkedHashMap<>();
        datos.put("MANUAL", 0);
        datos.put("AUTOMATICO", 0);

        for (Object[] fila : eventoRiegoRepository.contarRiegosPorModoMesActual()) {
            ModoRiego modo = (ModoRiego) fila[0];
            Long cantidad = (Long) fila[1];
            datos.put(modo.name(), cantidad.intValue());
        }

        return datos;
    }

    @Override
    public EstadisticasResumenResponse obtenerResumen() {
        Map<String, Integer> modos = obtenerDatosModosRiego();
        long manuales = modos.getOrDefault("MANUAL", 0);
        long automaticos = modos.getOrDefault("AUTOMATICO", 0);
        long totalMes = manuales + automaticos;
        long completados = eventoRiegoRepository.countByEstado(EstadoRiego.COMPLETADO);
        double humedadGanada = redondear(eventoRiegoRepository.obtenerPromedioHumedadGanada());
        String ultimoRiego = obtenerUltimoRiegoTexto();
        DatosDuracion duracion = obtenerDuracionUltimos7Dias();

        return new EstadisticasResumenResponse(
                totalMes,
                manuales,
                automaticos,
                completados,
                humedadGanada,
                ultimoRiego,
                duracion.labels(),
                duracion.valores()
        );
    }

    @Override
    public List<TelemetriaResponse> obtenerTelemetriaReciente() {
        return obtenerTelemetriaReciente(null, false);
    }

    @Override
    public List<TelemetriaResponse> obtenerTelemetriaReciente(Integer cultivoId, boolean soloMantenimiento) {
        List<LecturaSensor> lecturas = obtenerLecturasRecientes(cultivoId, soloMantenimiento);

        return lecturas
                .stream()
                .sorted(Comparator.comparing(LecturaSensor::getFechaLectura, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTelemetriaResponse)
                .toList();
    }

    @Override
    public DistribucionModosResponse obtenerDistribucionModosMesActual() {
        return obtenerDistribucionModosMesActual(null, false);
    }

    @Override
    public DistribucionModosResponse obtenerDistribucionModosMesActual(Integer cultivoId, boolean soloMantenimiento) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioMesSiguiente = inicioMes.plusMonths(1);

        long manual = contarRiegosPorFiltro(
                ModoRiego.MANUAL,
                cultivoId,
                soloMantenimiento,
                inicioMes,
                inicioMesSiguiente
        );
        long automatico = contarRiegosPorFiltro(
                ModoRiego.AUTOMATICO,
                cultivoId,
                soloMantenimiento,
                inicioMes,
                inicioMesSiguiente
        );

        return new DistribucionModosResponse(
                manual,
                automatico,
                manual + automatico,
                obtenerEtiquetaCultivo(cultivoId, soloMantenimiento)
        );
    }

    @Override
    public List<ConsumoAguaDetalleResponse> obtenerConsumoAguaDetalle(Integer cultivoId, boolean soloMantenimiento) {
        return eventoRiegoRepository.obtenerConsumoAguaDetalle(cultivoId, soloMantenimiento ? 1 : 0)
                .stream()
                .map(fila -> new ConsumoAguaDetalleResponse(
                        String.valueOf(fila[0]),
                        String.valueOf(fila[1]),
                        String.valueOf(fila[2]),
                        String.valueOf(fila[3]),
                        fila[4] instanceof Number numero ? numero.doubleValue() : 0.0
                ))
                .toList();
    }

    private List<LecturaSensor> obtenerLecturasRecientes(Integer cultivoId, boolean soloMantenimiento) {
        if (soloMantenimiento) {
            return lecturaSensorRepository.findTop20ByCultivoIdIsNullOrderByFechaLecturaDesc();
        }

        if (cultivoId != null) {
            return lecturaSensorRepository.findTop20ByCultivoIdOrderByFechaLecturaDesc(cultivoId);
        }

        return lecturaSensorRepository.findTop20ByOrderByFechaLecturaDesc();
    }

    private long contarRiegosPorFiltro(ModoRiego modoRiego,
                                       Integer cultivoId,
                                       boolean soloMantenimiento,
                                       LocalDateTime inicio,
                                       LocalDateTime fin) {
        if (soloMantenimiento) {
            return eventoRiegoRepository.countByModoRiegoAndCultivoIsNullAndFechaInicioBetween(modoRiego, inicio, fin);
        }

        if (cultivoId != null) {
            return eventoRiegoRepository.countByModoRiegoAndCultivo_IdAndFechaInicioBetween(modoRiego, cultivoId, inicio, fin);
        }

        return eventoRiegoRepository.countByModoRiegoAndFechaInicioBetween(modoRiego, inicio, fin);
    }

    private TelemetriaResponse toTelemetriaResponse(LecturaSensor lectura) {
        LocalDateTime fecha = lectura.getFechaLectura();

        return new TelemetriaResponse(
                fecha != null ? fecha.toString() : null,
                fecha != null ? fecha.format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--",
                lectura.getHumedadSuelo(),
                lectura.getDistanciaAgua()
        );
    }

    private String obtenerCultivoActivo() {
        return configuracionRiegoRepository.findById(1)
                .map(ConfiguracionRiego::getCultivoActivo)
                .map(cultivo -> cultivo.getNombre())
                .orElse("Sin cultivo");
    }

    private String obtenerEtiquetaCultivo(Integer cultivoId, boolean soloMantenimiento) {
        if (soloMantenimiento) {
            return "Pruebas / Mantenimiento";
        }

        if (cultivoId != null) {
            return perfilCultivoRepository.findById(cultivoId)
                    .map(Cultivo::getNombre)
                    .orElse("Cultivo " + cultivoId);
        }

        return obtenerCultivoActivo();
    }

    private String obtenerUltimoRiegoTexto() {
        EventoRiego ultimo = eventoRiegoRepository.findTopByOrderByFechaInicioDesc();
        if (ultimo == null || ultimo.getFechaInicio() == null) {
            return "Sin registros";
        }

        return ultimo.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    private DatosDuracion obtenerDuracionUltimos7Dias() {
        List<String> labels = new ArrayList<>();
        List<Long> valores = new ArrayList<>();

        for (Object[] fila : eventoRiegoRepository.obtenerDuracionDiariaUltimos7Dias()) {
            labels.add(formatearFecha(fila[0]));
            valores.add(toLong(fila[1]));
        }

        return new DatosDuracion(labels, valores);
    }

    private String formatearFecha(Object valor) {
        if (valor instanceof Date fecha) {
            return fecha.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM"));
        }

        return String.valueOf(valor);
    }

    private Long toLong(Object valor) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        return 0L;
    }

    private double redondear(Double valor) {
        if (valor == null) {
            return 0;
        }

        return Math.round(valor * 10.0) / 10.0;
    }

    private record DatosDuracion(List<String> labels, List<Long> valores) {
    }
}
