package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.dto.RiegoEstadoResponse;
import sri.project.sri_project.dto.RiegoManualRequest;
import sri.project.sri_project.dto.RiegoModoRequest;
import sri.project.sri_project.dto.RiegoPerfilRequest;
import sri.project.sri_project.dto.RiegoProgramacionRequest;
import sri.project.sri_project.model.enums.ModoOperacion;
import sri.project.sri_project.service.RiegoControlService;

import java.util.Map;

@RestController
@RequestMapping("/api/riego")
@RequiredArgsConstructor
public class RiegoApiController {

    private final RiegoControlService riegoControlService;

    @GetMapping("/estado")
    public RiegoEstadoResponse obtenerEstado() {
        return riegoControlService.obtenerEstado();
    }

    @PostMapping("/modo")
    public RiegoEstadoResponse cambiarModo(@RequestBody RiegoModoRequest request) {
        ModoOperacion modo = ModoOperacion.valueOf(request.modo().toUpperCase());
        return riegoControlService.cambiarModo(modo);
    }

    @PostMapping("/perfil")
    public RiegoEstadoResponse seleccionarPerfilAutomatico(@RequestBody RiegoPerfilRequest request) {
        return riegoControlService.seleccionarPerfilAutomatico(request.cultivoId());
    }

    @PostMapping("/programacion")
    public RiegoEstadoResponse programarRiegoAutomatico(@RequestBody RiegoProgramacionRequest request) {
        return riegoControlService.programarRiegoAutomatico(request.cultivoId(), request.horaRiego());
    }

    @PostMapping("/manual")
    public RiegoEstadoResponse ejecutarOrdenManual(@RequestBody RiegoManualRequest request) {
        if (request.cultivoId() == null) {
            throw new IllegalArgumentException("El cultivo es obligatorio para ejecutar un riego manual.");
        }

        return riegoControlService.ejecutarOrdenManual(request.orden(), request.cultivoId());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> manejarForbidden(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NullPointerException.class})
    public ResponseEntity<Map<String, String>> manejarBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
