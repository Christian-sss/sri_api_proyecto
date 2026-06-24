package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.dto.CultivoRequest;
import sri.project.sri_project.dto.CultivoResponse;
import sri.project.sri_project.service.PerfilCultivoService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cultivos")
@RequiredArgsConstructor
public class CultivoApiController {

    private final PerfilCultivoService perfilCultivoService;

    @GetMapping
    public List<CultivoResponse> listar() {
        return perfilCultivoService.listarTodos();
    }

    @GetMapping("/activos")
    public List<CultivoResponse> listarActivos() {
        return perfilCultivoService.listarActivos();
    }

    @GetMapping("/inactivos")
    public List<CultivoResponse> listarInactivos() {
        return perfilCultivoService.listarInactivos();
    }

    @GetMapping("/{id}")
    public CultivoResponse obtenerPorId(@PathVariable Integer id) {
        return perfilCultivoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<CultivoResponse> crear(@RequestBody CultivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilCultivoService.crear(request));
    }

    @PutMapping("/{id}")
    public CultivoResponse actualizar(@PathVariable Integer id, @RequestBody CultivoRequest request) {
        return perfilCultivoService.actualizar(id, request);
    }

    @PutMapping("/{id}/toggle-estado")
    public ResponseEntity<Void> toggleEstado(@PathVariable Integer id) {
        perfilCultivoService.toggleEstado(id);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarErrores(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
