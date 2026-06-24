package sri.project.sri_project.service;

import sri.project.sri_project.dto.CultivoRequest;
import sri.project.sri_project.dto.CultivoResponse;
import sri.project.sri_project.model.Cultivo;

import java.util.List;

public interface PerfilCultivoService {

    List<Cultivo> listarEntidadesActivas();

    List<Cultivo> listarTodasEntidades();

    List<CultivoResponse> listarTodos();

    List<CultivoResponse> listarActivos();

    List<CultivoResponse> listarInactivos();

    CultivoResponse obtenerPorId(Integer id);

    CultivoResponse crear(CultivoRequest request);

    CultivoResponse actualizar(Integer id, CultivoRequest request);

    void toggleEstado(Integer id);
}
