package sri.project.sri_project.service;

import net.sf.jasperreports.engine.JRException;

import java.io.FileNotFoundException;
import java.time.LocalDate;

public interface ReporteService {
    byte[] generarReporteModosRiegoPDF(LocalDate fechaInicio, LocalDate fechaFin, String cultivoId) throws JRException, FileNotFoundException;
    byte[] generarReporteConsumoAguaPDF(LocalDate fechaInicio, LocalDate fechaFin, String cultivoId) throws JRException, FileNotFoundException;
}
