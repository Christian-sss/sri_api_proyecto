package sri.project.sri_project.service.serviceImpl;

import lombok.AllArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import sri.project.sri_project.model.Cultivo;
import sri.project.sri_project.repository.PerfilCultivoRepository;
import sri.project.sri_project.service.ReporteService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class ReporteServiceImpl implements ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteServiceImpl.class);
    private static final String CULTIVO_MANTENIMIENTO = "mantenimiento";
    private static final String NOMBRE_MANTENIMIENTO = "Mantenimiento / Sin Cultivo";

    private final PerfilCultivoRepository perfilCultivoRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final sri.project.sri_project.repository.LecturaSensorRepository lecturaSensorRepository;

    @Override
    public byte[] generarReporteModosRiegoPDF(LocalDate fechaInicio, LocalDate fechaFin, String cultivoId)
            throws JRException, FileNotFoundException {

        FiltroReporte filtro = resolverFiltro(cultivoId);
        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.plusDays(1).atStartOfDay() : null;

        Collection<Map<String, ?>> datosReporte = new ArrayList<>();
        for (Map<String, Object> fila : obtenerFilasReporte(inicio, fin, filtro)) {
            datosReporte.add(toReporteRow(fila));
        }

        Collection<Map<String, ?>> resumenModos = new ArrayList<>();
        for (Map<String, Object> fila : obtenerResumenModos(inicio, fin, filtro)) {
            Map<String, Object> row = new HashMap<>();
            row.put("modoRiego", texto(valor(fila, "modoRiego", "modo_riego"), "-"));
            row.put("cantidad", toLong(valor(fila, "cantidad")));
            resumenModos.add(row);
        }

        // Add sensor data for the line chart
        List<sri.project.sri_project.model.LecturaSensor> ultimasLecturas;
        if (filtro != null && filtro.cultivoId() != null) {
            ultimasLecturas = lecturaSensorRepository.findTop20ByCultivoIdOrderByFechaLecturaDesc(filtro.cultivoId());
        } else {
            ultimasLecturas = lecturaSensorRepository.findTop20ByOrderByFechaLecturaDesc();
        }

        Collection<Map<String, ?>> sensorDataList = new ArrayList<>();
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        // Reverse to show chronological order in the chart
        for (int i = ultimasLecturas.size() - 1; i >= 0; i--) {
            sri.project.sri_project.model.LecturaSensor lectura = ultimasLecturas.get(i);
            Map<String, Object> row = new HashMap<>();
            row.put("fecha", lectura.getFechaLectura().format(timeFormatter));
            row.put("humedad", lectura.getHumedadSuelo() != null ? lectura.getHumedadSuelo().doubleValue() : 0.0);
            sensorDataList.add(row);
        }

        try {
            // Compila la plantilla desde resources, incluso dentro de un WAR/JAR desplegado.
            JasperReport jasperReport = cargarReporteDesdeJrxml("reportes/grafico_modos_riego.jrxml");
            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(datosReporte);

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("creadoPor", "Sistema SRI - Administrador");
            parametros.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            parametros.put("TITULO_FILTRO", obtenerTituloFiltro(filtro));
            parametros.put("sensorDataSource", new net.sf.jasperreports.engine.data.JRMapCollectionDataSource(sensorDataList));
            parametros.put("modosDataSource", new JRMapCollectionDataSource(resumenModos));

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException | FileNotFoundException e) {
            // Conserva el error original y deja el detalle disponible en los logs del servidor.
            log.error("Error al generar el reporte operativo: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public byte[] generarReporteConsumoAguaPDF(LocalDate fechaInicio, LocalDate fechaFin, String cultivoId)
            throws JRException, FileNotFoundException {
        FiltroReporte filtro = resolverFiltro(cultivoId);
        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.plusDays(1).atStartOfDay() : null;

        Collection<Map<String, ?>> datosReporte = new ArrayList<>();
        double totalLitros = 0.0;

        for (Map<String, Object> fila : obtenerFilasConsumoAgua(inicio, fin, filtro)) {
            Map<String, Object> row = toConsumoAguaRow(fila);
            datosReporte.add(row);
            totalLitros += ((Number) row.get("litrosConsumidos")).doubleValue();
        }

        try {
            // Compila la plantilla desde resources para no depender de archivos locales precompilados.
            JasperReport jasperReport = cargarReporteDesdeJrxml("reportes/consumo_agua.jrxml");
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("creadoPor", "Sistema SRI - Administrador");
            parametros.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            parametros.put("TITULO_FILTRO", obtenerTituloFiltro(filtro));
            parametros.put("TOTAL_LITROS", Math.round(totalLitros * 100.0) / 100.0);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    parametros,
                    new JRMapCollectionDataSource(datosReporte)
            );
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException | FileNotFoundException e) {
            // Registra el mensaje exacto y la traza antes de propagar el error al controlador.
            log.error("Error al generar el reporte de consumo de agua: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Abre y compila una plantilla mediante classpath, sin depender de rutas del sistema operativo.
    private JasperReport cargarReporteDesdeJrxml(String rutaClasspath)
            throws JRException, FileNotFoundException {
        ClassPathResource recurso = new ClassPathResource(rutaClasspath);

        // El try-with-resources cierra el stream automaticamente despues de compilar el reporte.
        try (InputStream reporteStream = recurso.getInputStream()) {
            return JasperCompileManager.compileReport(reporteStream);
        } catch (IOException e) {
            // Traduce errores de lectura o recursos ausentes manteniendo la causa original.
            log.error("No se pudo leer la plantilla de reporte '{}': {}", rutaClasspath, e.getMessage(), e);
            FileNotFoundException exception = new FileNotFoundException(
                    "No se pudo cargar la plantilla de reporte desde el classpath: " + rutaClasspath
            );
            exception.initCause(e);
            throw exception;
        } catch (JRException e) {
            // Identifica plantillas invalidas o incompatibles con la version actual.
            log.error("La plantilla '{}' no se pudo compilar: {}", rutaClasspath, e.getMessage(), e);
            throw e;
        }
    }

    private List<Map<String, Object>> obtenerFilasConsumoAgua(LocalDateTime inicio,
                                                              LocalDateTime fin,
                                                              FiltroReporte filtro) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    DATE_FORMAT(e.fecha_inicio, '%Y-%m-%d') AS fecha,
                    COALESCE(c.nombre, 'Mantenimiento / Sin Cultivo') AS nombreCultivo,
                    ROUND(SUM(TIMESTAMPDIFF(SECOND, e.fecha_inicio, e.fecha_fin)) / 60.0, 2) AS duracionMinutos,
                    ROUND((SUM(TIMESTAMPDIFF(SECOND, e.fecha_inicio, e.fecha_fin)) / 60.0) * 1.0, 2) AS litrosConsumidos
                FROM eventos_riego e
                LEFT JOIN perfiles_cultivo c ON c.id = e.cultivo_id
                WHERE e.estado = 'COMPLETADO' AND e.fecha_fin IS NOT NULL
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (inicio != null) {
            sql.append(" AND e.fecha_inicio >= :fechaInicio");
            params.addValue("fechaInicio", inicio);
        }

        if (fin != null) {
            sql.append(" AND e.fecha_inicio < :fechaFin");
            params.addValue("fechaFin", fin);
        }

        if (filtro.soloMantenimiento()) {
            sql.append(" AND e.cultivo_id IS NULL");
        } else if (filtro.cultivoId() != null) {
            sql.append(" AND e.cultivo_id = :cultivoId");
            params.addValue("cultivoId", filtro.cultivoId());
        }

        sql.append(" GROUP BY DATE(e.fecha_inicio), e.cultivo_id, c.nombre");
        sql.append(" ORDER BY DATE(e.fecha_inicio) DESC, c.nombre");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private Map<String, Object> toConsumoAguaRow(Map<String, Object> fila) {
        Map<String, Object> row = new HashMap<>();
        row.put("fecha", texto(valor(fila, "fecha"), "-"));
        row.put("nombreCultivo", texto(valor(fila, "nombreCultivo", "nombre_cultivo"), NOMBRE_MANTENIMIENTO));
        row.put("duracionMinutos", numero(valor(fila, "duracionMinutos", "duracion_minutos")));
        row.put("litrosConsumidos", numero(valor(fila, "litrosConsumidos", "litros_consumidos")));
        return row;
    }

    private Double numero(Object valor) {
        return valor instanceof Number numero ? numero.doubleValue() : 0.0;
    }

    private List<Map<String, Object>> obtenerFilasReporte(LocalDateTime inicio, LocalDateTime fin, FiltroReporte filtro) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.id AS id,
                    e.fecha_inicio AS fechaInicio,
                    COALESCE(c.nombre, 'Mantenimiento / Sin Cultivo') AS nombreCultivo,
                    e.modo_riego AS modoRiego,
                    e.humedad_suelo_inicial AS humedadSueloInicial,
                    e.humedad_suelo_final AS humedadSueloFinal,
                    e.estado AS estado,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM lecturas_sensor ls
                            WHERE ls.cultivo_id = e.cultivo_id
                              AND ls.fecha_lectura BETWEEN e.fecha_inicio AND COALESCE(e.fecha_fin, NOW())
                              AND ls.distancia_agua >= 18.0
                        ) THEN 'Bomba Bloqueada'
                        ELSE 'Optimo'
                    END AS estadoUltrasonico,
                    1 AS cantidad
                FROM eventos_riego e
                LEFT JOIN perfiles_cultivo c ON c.id = e.cultivo_id
                WHERE 1 = 1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (inicio != null) {
            sql.append(" AND e.fecha_inicio >= :fechaInicio");
            params.addValue("fechaInicio", inicio);
        }

        if (fin != null) {
            sql.append(" AND e.fecha_inicio < :fechaFin");
            params.addValue("fechaFin", fin);
        }

        if (filtro.soloMantenimiento()) {
            sql.append(" AND e.cultivo_id IS NULL");
        } else if (filtro.cultivoId() != null) {
            sql.append(" AND e.cultivo_id = :cultivoId");
            params.addValue("cultivoId", filtro.cultivoId());
        }

        sql.append(" ORDER BY e.fecha_inicio DESC");

        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> obtenerResumenModos(LocalDateTime inicio,
                                                          LocalDateTime fin,
                                                          FiltroReporte filtro) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.modo_riego AS modoRiego,
                    COUNT(e.id) AS cantidad
                FROM eventos_riego e
                WHERE 1 = 1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (inicio != null) {
            sql.append(" AND e.fecha_inicio >= :fechaInicio");
            params.addValue("fechaInicio", inicio);
        }

        if (fin != null) {
            sql.append(" AND e.fecha_inicio < :fechaFin");
            params.addValue("fechaFin", fin);
        }

        if (filtro.soloMantenimiento()) {
            sql.append(" AND e.cultivo_id IS NULL");
        } else if (filtro.cultivoId() != null) {
            sql.append(" AND e.cultivo_id = :cultivoId");
            params.addValue("cultivoId", filtro.cultivoId());
        }

        sql.append(" GROUP BY e.modo_riego ORDER BY e.modo_riego");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private Map<String, Object> toReporteRow(Map<String, Object> fila) {
        Map<String, Object> row = new HashMap<>();

        row.put("id", valor(fila, "id"));
        row.put("fechaInicio", formatearFecha(valor(fila, "fechaInicio", "fecha_inicio")));
        row.put("nombreCultivo", texto(valor(fila, "nombreCultivo", "nombre_cultivo"), NOMBRE_MANTENIMIENTO));
        row.put("modoRiego", texto(valor(fila, "modoRiego", "modo_riego"), "-"));
        row.put("humedadSueloInicial", valor(fila, "humedadSueloInicial", "humedad_suelo_inicial") != null
                ? valor(fila, "humedadSueloInicial", "humedad_suelo_inicial")
                : "-");
        row.put("humedadSueloFinal", valor(fila, "humedadSueloFinal", "humedad_suelo_final") != null
                ? valor(fila, "humedadSueloFinal", "humedad_suelo_final")
                : "-");
        row.put("estado", texto(valor(fila, "estado"), "-"));
        row.put("estadoUltrasonico", texto(valor(fila, "estadoUltrasonico", "estado_ultrasonico"), "Optimo"));
        row.put("cantidad", valor(fila, "cantidad") != null ? valor(fila, "cantidad") : 1);

        return row;
    }

    private Object valor(Map<String, Object> fila, String... claves) {
        for (String clave : claves) {
            if (fila.containsKey(clave)) {
                return fila.get(clave);
            }
        }

        return null;
    }

    private String texto(Object valor, String valorPorDefecto) {
        return valor != null ? String.valueOf(valor) : valorPorDefecto;
    }

    private Long toLong(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : 0L;
    }

    private String formatearFecha(Object valor) {
        if (valor == null) {
            return "-";
        }

        if (valor instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        if (valor instanceof LocalDateTime fecha) {
            return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        return String.valueOf(valor);
    }

    private FiltroReporte resolverFiltro(String cultivoId) {
        if (cultivoId == null || cultivoId.isBlank()) {
            return new FiltroReporte(null, false);
        }

        if (CULTIVO_MANTENIMIENTO.equalsIgnoreCase(cultivoId.trim())) {
            return new FiltroReporte(null, true);
        }

        return new FiltroReporte(Integer.valueOf(cultivoId), false);
    }

    private String obtenerTituloFiltro(FiltroReporte filtro) {
        if (filtro.soloMantenimiento()) {
            return NOMBRE_MANTENIMIENTO;
        }

        if (filtro.cultivoId() != null) {
            return perfilCultivoRepository.findById(filtro.cultivoId())
                    .map(Cultivo::getNombre)
                    .orElse("Cultivo " + filtro.cultivoId());
        }

        return "Reporte General";
    }

    private record FiltroReporte(Integer cultivoId, boolean soloMantenimiento) {
    }
}
