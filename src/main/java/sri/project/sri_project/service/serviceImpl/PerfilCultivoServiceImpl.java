package sri.project.sri_project.service.serviceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sri.project.sri_project.dto.CultivoRequest;
import sri.project.sri_project.dto.CultivoResponse;
import sri.project.sri_project.model.Cultivo;
import sri.project.sri_project.repository.PerfilCultivoRepository;
import sri.project.sri_project.service.PerfilCultivoService;

import java.util.List;

@Service
public class PerfilCultivoServiceImpl implements PerfilCultivoService {

    private final PerfilCultivoRepository perfilCultivoRepository;

    public PerfilCultivoServiceImpl(PerfilCultivoRepository perfilCultivoRepository) {
        this.perfilCultivoRepository = perfilCultivoRepository;
    }

    @Override
    public List<Cultivo> listarEntidadesActivas() {
        return perfilCultivoRepository.findByActivoTrue();
    }

    @Override
    public List<Cultivo> listarTodasEntidades() {
        return perfilCultivoRepository.findAll();
    }

    @Override
    public List<CultivoResponse> listarTodos() {
        return perfilCultivoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CultivoResponse> listarActivos() {
        return perfilCultivoRepository.findByActivoTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CultivoResponse> listarInactivos() {
        return perfilCultivoRepository.findByActivoFalse()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CultivoResponse obtenerPorId(Integer id) {
        Cultivo cultivo = perfilCultivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        return toResponse(cultivo);
    }

    @Override
    @Transactional
    public CultivoResponse crear(CultivoRequest request) {
        validar(request);
        validarNombreUnico(request.nombre(), null);

        Cultivo cultivo = new Cultivo();
        aplicarDatos(cultivo, request);
        cultivo.setActivo(true);

        return toResponse(perfilCultivoRepository.save(cultivo));
    }

    @Override
    @Transactional
    public CultivoResponse actualizar(Integer id, CultivoRequest request) {
        validar(request);
        validarNombreUnico(request.nombre(), id);

        Cultivo cultivo = perfilCultivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        aplicarDatos(cultivo, request);
        return toResponse(perfilCultivoRepository.save(cultivo));
    }

    @Override
    @Transactional
    public void toggleEstado(Integer id) {
        Cultivo cultivo = perfilCultivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        cultivo.setActivo(!Boolean.TRUE.equals(cultivo.getActivo()));
        perfilCultivoRepository.save(cultivo);
    }

    private void aplicarDatos(Cultivo cultivo, CultivoRequest request) {
        cultivo.setNombre(request.nombre().trim());
        cultivo.setHumedadMinOptima(request.humedadMinOptima());
        cultivo.setHumedadMaxOptima(request.humedadMaxOptima());
        cultivo.setDuracionRiegoMinutos(request.duracionRiegoMinutos());
        cultivo.setTratoRecomendado(request.tratoRecomendado());
    }

    private void validar(CultivoRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cultivo es obligatorio.");
        }

        if (request.nombre().matches(".*\\d.*")) {
            throw new IllegalArgumentException("El nombre del cultivo no puede contener números.");
        }

        if (request.humedadMinOptima() == null || request.humedadMaxOptima() == null) {
            throw new IllegalArgumentException("La humedad mínima y máxima son obligatorias.");
        }

        if (request.duracionRiegoMinutos() == null || request.duracionRiegoMinutos() <= 0) {
            throw new IllegalArgumentException("La duracion de riego debe ser mayor a 0 minutos.");
        }

        if (request.humedadMinOptima() < 0
                || request.humedadMinOptima() > 100
                || request.humedadMaxOptima() < 0
                || request.humedadMaxOptima() > 100) {
            throw new IllegalArgumentException("La humedad debe estar entre 0 y 100.");
        }

        if (request.humedadMinOptima() >= request.humedadMaxOptima()) {
            throw new IllegalArgumentException("La humedad mínima debe ser menor que la máxima.");
        }

    }

    private void validarNombreUnico(String nombre, Integer cultivoId) {
        String nombreNormalizado = nombre.trim();
        boolean nombreExistente = cultivoId == null
                ? perfilCultivoRepository.existsByNombreIgnoreCase(nombreNormalizado)
                : perfilCultivoRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, cultivoId);

        if (nombreExistente) {
            throw new IllegalArgumentException("Ya existe un perfil de cultivo con ese nombre.");
        }
    }

    private CultivoResponse toResponse(Cultivo cultivo) {
        return new CultivoResponse(
                cultivo.getId(),
                cultivo.getNombre(),
                cultivo.getHumedadMinOptima(),
                cultivo.getHumedadMaxOptima(),
                cultivo.getDuracionRiegoMinutos(),
                cultivo.getTratoRecomendado(),
                cultivo.getActivo()
        );
    }
}
