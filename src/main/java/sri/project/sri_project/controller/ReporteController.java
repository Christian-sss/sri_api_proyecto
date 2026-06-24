package sri.project.sri_project.controller;

import lombok.AllArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.service.ReporteService;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import jakarta.servlet.http.HttpSession;
import sri.project.sri_project.model.User;
import sri.project.sri_project.service.EmailService;

@AllArgsConstructor
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final EmailService emailService;

    @GetMapping("/descargar-pdf")
    public ResponseEntity<byte[]> descargarReporteRiegos(@RequestParam(required = false) LocalDate fechaInicio,
                                                         @RequestParam(required = false) LocalDate fechaFin,
                                                         @RequestParam(required = false) String cultivoId,
                                                         HttpSession session) {
        try {
            byte[] reportePdf = reporteService.generarReporteModosRiegoPDF(fechaInicio, fechaFin, cultivoId);

            User usuarioLogueado = (User) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null && usuarioLogueado.getEmail() != null) {
                new Thread(() -> {
                    try {
                        emailService.enviarReportePorCorreoConAdjunto(usuarioLogueado.getEmail(), "Reporte de Sistema de Riego", reportePdf);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        try {
                            java.nio.file.Files.writeString(java.nio.file.Path.of("mail_error.log"), "Reporte Thread Error: " + ex.getMessage(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                        } catch (Exception ignored) {}
                    }
                }).start();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Reporte_Sistema_Riego.pdf");
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(reportePdf.length);

            return new ResponseEntity<>(reportePdf, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (JRException | FileNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            String mensaje = "Error al generar el reporte PDF: " + e.getClass().getName() + " - " + e.getMessage();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>(mensaje.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/consumo-agua")
    public ResponseEntity<byte[]> descargarReporteConsumoAgua(
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @RequestParam(required = false) String cultivoId,
            HttpSession session) {
        try {
            byte[] reportePdf = reporteService.generarReporteConsumoAguaPDF(fechaInicio, fechaFin, cultivoId);

            User usuarioLogueado = (User) session.getAttribute("usuarioLogueado");
            if (usuarioLogueado != null && usuarioLogueado.getEmail() != null) {
                new Thread(() -> {
                    try {
                        emailService.enviarReportePorCorreoConAdjunto(
                                usuarioLogueado.getEmail(),
                                "Reporte de Consumo de Agua",
                                reportePdf
                        );
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "Reporte_Consumo_Agua.pdf");
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(reportePdf.length);
            return new ResponseEntity<>(reportePdf, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (JRException | FileNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            String mensaje = "Error al generar el reporte de consumo de agua: "
                    + e.getClass().getName() + " - " + e.getMessage();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>(mensaje.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail(@RequestParam(defaultValue = "julcortezm.jys@gmail.com") String email) {
        try {
            emailService.enviarCorreoLogin(email, "Prueba Test");
            return ResponseEntity.ok("Correo de prueba enviado exitosamente a " + email);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of("mail_error.log"), "Test Error: " + e.getMessage(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
            return ResponseEntity.status(500).body("Error enviando correo: " + e.getMessage());
        }
    }
}
