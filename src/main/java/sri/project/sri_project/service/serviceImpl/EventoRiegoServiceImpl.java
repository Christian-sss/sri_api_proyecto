package sri.project.sri_project.service.serviceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.model.Cultivo;
import sri.project.sri_project.model.EventoRiego;
import sri.project.sri_project.model.enums.EstadoRiego;
import sri.project.sri_project.model.enums.ModoRiego;
import sri.project.sri_project.repository.EventoRiegoRepository;
import sri.project.sri_project.repository.PerfilCultivoRepository;
import sri.project.sri_project.service.EventoRiegoService;

import java.time.LocalDateTime;

@Service
public class EventoRiegoServiceImpl implements EventoRiegoService {

    private final EventoRiegoRepository eventoRiegoRepository;
    private final PerfilCultivoRepository perfilCultivoRepository;

    public EventoRiegoServiceImpl(EventoRiegoRepository eventoRiegoRepository,
                                  PerfilCultivoRepository perfilCultivoRepository) {
        this.eventoRiegoRepository = eventoRiegoRepository;
        this.perfilCultivoRepository = perfilCultivoRepository;
    }

    @Override
    @Transactional
    public void registrarInicio(Integer cultivoId, ModoRiego modoRiego, SensorData lecturaInicial) {
        if (cultivoId == null) {
            throw new IllegalArgumentException("El cultivo es obligatorio para registrar el evento de riego.");
        }

        if (modoRiego == null || lecturaInicial == null) {
            throw new IllegalArgumentException("El modo y la lectura inicial son obligatorios.");
        }

        Cultivo cultivo = perfilCultivoRepository.findById(cultivoId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        if (eventoRiegoRepository.findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego.EN_PROCESO).isPresent()) {
            System.out.println("[EVENTO-RIEGO] Ya existe un riego en proceso. No se crea otro evento.");
            return;
        }

        EventoRiego evento = new EventoRiego();
        evento.setCultivo(cultivo);
        evento.setModoRiego(modoRiego);
        evento.setFechaInicio(LocalDateTime.now());
        evento.setHumedadSueloInicial(lecturaInicial.humedad());
        evento.setEstado(EstadoRiego.EN_PROCESO);

        eventoRiegoRepository.save(evento);
        System.out.println("[EVENTO-RIEGO] Inicio registrado para " + cultivo.getNombre() + " en modo " + modoRiego + ".");
    }

    @Override
    @Transactional
    public void completarRiego(SensorData lecturaFinal) {
        eventoRiegoRepository.findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego.EN_PROCESO)
                .ifPresentOrElse(evento -> completarEvento(evento, lecturaFinal), () ->
                        System.out.println("[EVENTO-RIEGO] No hay riego en proceso para completar."));
    }

    private void completarEvento(EventoRiego evento, SensorData lecturaFinal) {
        evento.setFechaFin(LocalDateTime.now());
        evento.setHumedadSueloFinal(lecturaFinal != null ? lecturaFinal.humedad() : evento.getHumedadSueloInicial());
        evento.setEstado(EstadoRiego.COMPLETADO);

        eventoRiegoRepository.save(evento);
        System.out.println("[EVENTO-RIEGO] Riego completado. Evento ID: " + evento.getId());
    }
}
