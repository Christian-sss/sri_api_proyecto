package sri.project.sri_project.service.serviceImpl;

import org.springframework.stereotype.Service;
import sri.project.sri_project.model.ConfiguracionRiego;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.model.EventoRiego;
import sri.project.sri_project.model.LecturaSensor;
import sri.project.sri_project.model.Policy.SeguridadHidrica;
import sri.project.sri_project.model.TanqueAgua;
import sri.project.sri_project.model.enums.EstadoRiego;
import sri.project.sri_project.repository.ConfiguracionRiegoRepository;
import sri.project.sri_project.repository.EventoRiegoRepository;
import sri.project.sri_project.repository.LecturaSensorRepository;

import java.util.Optional;

@Service
public class ProcesarDatosService {

    private final TanqueAgua tanque;
    private final SeguridadHidrica seguridadHidrica;
    private final LecturaSensorRepository lecturaSensorRepository;
    private final ConfiguracionRiegoRepository configuracionRiegoRepository;
    private final EventoRiegoRepository eventoRiegoRepository;

    public ProcesarDatosService(
            TanqueAgua tanque,
            SeguridadHidrica seguridadHidrica,
            LecturaSensorRepository lecturaSensorRepository,
            ConfiguracionRiegoRepository configuracionRiegoRepository,
            EventoRiegoRepository eventoRiegoRepository
    ) {
        this.tanque = tanque;
        this.seguridadHidrica = seguridadHidrica;
        this.lecturaSensorRepository = lecturaSensorRepository;
        this.configuracionRiegoRepository = configuracionRiegoRepository;
        this.eventoRiegoRepository = eventoRiegoRepository;
    }

    public void procesar(SensorData data) {
        guardarLectura(data);

        tanque.actualizarDatos(
                data.humedad(),
                data.distancia()
        );

        seguridadHidrica.evaluarEstado(tanque);
    }

    private void guardarLectura(SensorData data) {
        LecturaSensor lectura = new LecturaSensor();
        lectura.setHumedadSuelo(data.humedad());
        lectura.setDistanciaAgua(data.distancia());
        lectura.setCultivoId(obtenerCultivoLecturaId().orElse(null));

        lecturaSensorRepository.save(lectura);
    }

    private Optional<Integer> obtenerCultivoLecturaId() {
        Optional<Integer> cultivoEnRiego = eventoRiegoRepository
                .findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego.EN_PROCESO)
                .map(EventoRiego::getCultivo)
                .map(cultivo -> cultivo.getId());

        return cultivoEnRiego.isPresent() ? cultivoEnRiego : obtenerCultivoActivoId();
    }

    private Optional<Integer> obtenerCultivoActivoId() {
        return configuracionRiegoRepository.findById(1)
                .map(ConfiguracionRiego::getCultivoActivo)
                .map(cultivo -> cultivo.getId());
    }
}
