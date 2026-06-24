package sri.project.sri_project.service.serviceImpl;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sri.project.sri_project.dto.RiegoEstadoResponse;
import sri.project.sri_project.event.RiegoFinalizadoEvent;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.integration.ControlRiego;
import sri.project.sri_project.integration.Esp32MqttSensor;
import sri.project.sri_project.model.ConfiguracionRiego;
import sri.project.sri_project.model.Cultivo;
import sri.project.sri_project.model.enums.ModoOperacion;
import sri.project.sri_project.model.enums.ModoRiego;
import sri.project.sri_project.repository.ConfiguracionRiegoRepository;
import sri.project.sri_project.repository.PerfilCultivoRepository;
import sri.project.sri_project.service.EventoRiegoService;
import sri.project.sri_project.service.MotorRiegoService;
import sri.project.sri_project.service.RiegoControlService;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Service
public class RiegoControlServiceImpl implements RiegoControlService {

    private static final int CONFIG_ID = 1;
    private static final int COMANDO_OFF = 0;
    private static final int COMANDO_ON = 1;

    private final ConfiguracionRiegoRepository configuracionRiegoRepository;
    private final PerfilCultivoRepository perfilCultivoRepository;
    private final ControlRiego controlRiego;
    private final Esp32MqttSensor esp32MqttSensor;
    private final EventoRiegoService eventoRiegoService;
    private final MotorRiegoService motorRiegoService;

    public RiegoControlServiceImpl(
            ConfiguracionRiegoRepository configuracionRiegoRepository,
            PerfilCultivoRepository perfilCultivoRepository,
            ControlRiego controlRiego,
            Esp32MqttSensor esp32MqttSensor,
            EventoRiegoService eventoRiegoService,
            MotorRiegoService motorRiegoService
    ) {
        this.configuracionRiegoRepository = configuracionRiegoRepository;
        this.perfilCultivoRepository = perfilCultivoRepository;
        this.controlRiego = controlRiego;
        this.esp32MqttSensor = esp32MqttSensor;
        this.eventoRiegoService = eventoRiegoService;
        this.motorRiegoService = motorRiegoService;
    }

    @Override
    public RiegoEstadoResponse obtenerEstado() {
        ConfiguracionRiego configuracion = obtenerConfiguracion();
        return toResponse(configuracion, "Estado de riego consultado.");
    }

    @Override
    @Transactional
    public RiegoEstadoResponse cambiarModo(ModoOperacion modoOperacion) {
        ConfiguracionRiego configuracion = obtenerConfiguracion();
        configuracion.setModoOperacion(modoOperacion);
        configuracionRiegoRepository.save(configuracion);

        return toResponse(configuracion, "Modo de operación actualizado.");
    }

    @Override
    @Transactional
    public RiegoEstadoResponse seleccionarPerfilAutomatico(Integer cultivoId) {
        ConfiguracionRiego configuracion = obtenerConfiguracion();
        Cultivo cultivo = obtenerCultivoActivo(cultivoId);

        configuracion.setCultivoActivo(cultivo);
        configuracionRiegoRepository.save(configuracion);

        return toResponse(configuracion, "Perfil automático seleccionado.");
    }

    @Override
    @Transactional
    public RiegoEstadoResponse programarRiegoAutomatico(Integer cultivoId, String horaRiego) {
        if (cultivoId == null) {
            throw new IllegalArgumentException("El cultivo es obligatorio.");
        }

        if (horaRiego == null || horaRiego.isBlank()) {
            throw new IllegalArgumentException("La hora de riego es obligatoria.");
        }

        ConfiguracionRiego configuracion = obtenerConfiguracion();
        Cultivo cultivo = obtenerCultivoActivo(cultivoId);

        configuracion.setCultivoActivo(cultivo);
        configuracion.setModoOperacion(ModoOperacion.AUTOMATICO);
        configuracion.setHoraRiegoProgramada(parseHoraRiego(horaRiego));
        configuracionRiegoRepository.saveAndFlush(configuracion);
        motorRiegoService.resetearProgramacion();

        return toResponse(configuracion, "Programación automática guardada.");
    }

    @Override
    @Transactional
    public RiegoEstadoResponse ejecutarOrdenManual(String orden, Integer cultivoId) {
        ModoOperacion modoActual = obtenerModoActual();
        if (modoActual == ModoOperacion.AUTOMATICO) {
            throw new SecurityException("El sistema está en automático. La orden manual fue rechazada.");
        }

        if (cultivoId == null) {
            throw new IllegalArgumentException("El cultivo es obligatorio para ejecutar un riego manual.");
        }

        Cultivo cultivoSeleccionado = obtenerCultivoActivo(cultivoId);

        int comando = convertirOrdenAComando(orden);
        SensorData lecturaInicial = comando == COMANDO_ON ? esp32MqttSensor.getUltimoDato() : null;
        if (comando == COMANDO_ON && lecturaInicial == null) {
            throw new IllegalArgumentException("No hay una lectura de sensor disponible para iniciar el riego.");
        }

        ConfiguracionRiego configuracion = obtenerConfiguracion();
        if (comando == COMANDO_ON) {
            configuracion.setCultivoActivo(cultivoSeleccionado);
            configuracionRiegoRepository.saveAndFlush(configuracion);
        }

        controlRiego.enviarComando(comando);

        if (comando == COMANDO_ON) {
            registrarInicioManual(cultivoId, lecturaInicial);
        } else {
            eventoRiegoService.completarRiego(esp32MqttSensor.getUltimoDato());
            motorRiegoService.detenerRiegoActivo();
        }

        return toResponse(configuracion, comando == COMANDO_ON ? "Bomba encendida." : "Bomba apagada.");
    }

    @Override
    public ModoOperacion obtenerModoActual() {
        return obtenerConfiguracion().getModoOperacion();
    }

    private ConfiguracionRiego obtenerConfiguracion() {
        return configuracionRiegoRepository.findById(CONFIG_ID)
                .orElseGet(this::crearConfiguracionInicial);
    }

    private ConfiguracionRiego crearConfiguracionInicial() {
        ConfiguracionRiego configuracion = new ConfiguracionRiego();
        configuracion.setId(CONFIG_ID);
        configuracion.setModoOperacion(ModoOperacion.MANUAL);
        return configuracionRiegoRepository.save(configuracion);
    }

    private int convertirOrdenAComando(String orden) {

        if ("ON".equalsIgnoreCase(orden)) {
            return COMANDO_ON;
        }

        if ("OFF".equalsIgnoreCase(orden)) {
            return COMANDO_OFF;
        }

        throw new IllegalArgumentException("Orden de riego inválida.");
    }

    private void registrarInicioManual(Integer cultivoId, SensorData lecturaInicial) {
        eventoRiegoService.registrarInicio(cultivoId, ModoRiego.MANUAL, lecturaInicial);
    }

    private Cultivo obtenerCultivoActivo(Integer cultivoId) {
        if (cultivoId == null) {
            throw new IllegalArgumentException("El cultivo es obligatorio.");
        }

        Cultivo cultivo = perfilCultivoRepository.findById(cultivoId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de cultivo no encontrado."));

        if (!Boolean.TRUE.equals(cultivo.getActivo())) {
            throw new IllegalArgumentException("El perfil de cultivo esta inactivo y no puede usarse para riego.");
        }

        return cultivo;
    }

    private LocalTime parseHoraRiego(String horaRiego) {
        try {
            return LocalTime.parse(horaRiego);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("La hora de riego debe tener formato HH:mm.");
        }
    }

    @EventListener
    public void onRiegoFinalizado(RiegoFinalizadoEvent event) {
        if ("tanque_vacio".equals(event.getMotivo())) {
            eventoRiegoService.completarRiego(event.getLecturaFinal());
            motorRiegoService.detenerRiegoActivo();
        }

        System.out.println("[CONTROL-RIEGO] Riego finalizado por: " + event.getMotivo());
    }

    private RiegoEstadoResponse toResponse(ConfiguracionRiego configuracion, String mensaje) {
        ModoOperacion modo = configuracion.getModoOperacion();
        Cultivo cultivo = configuracion.getCultivoActivo();

        return new RiegoEstadoResponse(
                modo.name(),
                modo == ModoOperacion.AUTOMATICO,
                cultivo != null ? cultivo.getId() : null,
                cultivo != null ? cultivo.getNombre() : null,
                configuracion.getHoraRiegoProgramada() != null ? configuracion.getHoraRiegoProgramada().toString() : null,
                mensaje
        );
    }
}
