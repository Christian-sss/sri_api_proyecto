package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.dto.CultivoResponse;
import sri.project.sri_project.service.PerfilCultivoService;

import java.util.List;

@RestController
@RequestMapping("/api/riego")
@RequiredArgsConstructor
public class RiegoController {

    private final PerfilCultivoService perfilCultivoService;

    @GetMapping("/cultivos-activos")
    public List<CultivoResponse> listarCultivosActivos() {
        return perfilCultivoService.listarActivos();
    }
}
