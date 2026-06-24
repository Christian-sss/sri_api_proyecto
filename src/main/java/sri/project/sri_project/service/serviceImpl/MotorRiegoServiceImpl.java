package sri.project.sri_project.service.serviceImpl;

import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sri.project.sri_project.dto.SensorData;
import sri.project.sri_project.event.RiegoFinalizadoEvent;
import sri.project.sri_project.integration.ControlRiego;
import sri.project.sri_project.integration.Esp32MqttSensor;
import sri.project.sri_project.model.ConfiguracionRiego;
import sri.project.sri_project.model.Cultivo;
import sri.project.sri_project.model.enums.ModoOperacion;
import sri.project.sri_project.model.enums.ModoRiego;
import sri.project.sri_project.repository.ConfiguracionRiegoRepository;
import sri.project.sri_project.service.EventoRiegoService;
import sri.project.sri_project.service.MotorRiegoService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Service
public class MotorRiegoServiceImpl implements MotorRiegoService {

    private static final int CONFIG_ID = 1;
    private static final int COMANDO_OFF = 0;
    private static final int COMANDO_ON = 1;

    private final ConfiguracionRiegoRepository configuracionRiegoRepository;
    private final Esp32MqttSensor esp32MqttSensor;
    private final ControlRiego controlRiego;
    private final TaskScheduler taskScheduler;
    private final EventoRiegoService eventoRiegoService;
    private final ApplicationEventPublisher eventPublisher;

    private boolean riegoAutomaticoActivo = false;
    private LocalDate ultimaEjecucionProgramada;
    private ScheduledFuture<?> apagadoProgramado;

    public MotorRiegoServiceImpl(
            ConfiguracionRiegoRepository configuracionRiegoRepository,
            Esp32MqttSensor esp32MqttSensor,
            ControlRiego controlRiego,
            TaskScheduler taskScheduler,
            EventoRiegoService eventoRiegoService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.configuracionRiegoRepository = configuracionRiegoRepository;
        this.esp32MqttSensor = esp32MqttSensor;
        this.controlRiego = controlRiego;
        this.taskScheduler = taskScheduler;
        this.eventoRiegoService = eventoRiegoService;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void configurarCallbackSensor() {
        esp32MqttSensor.setOnDataReceived(this::verificarMaximoEnDatosSensor);
    }

    private void verificarMaximoEnDatosSensor(SensorData data) {
        if (!riegoAutomaticoActivo) {
            return;
        }

        Optional<Cultivo> cultivoOpt = obtenerCultivoAutomaticoSeleccionado();
        if (cultivoOpt.isEmpty()) {
            return;
        }

        Cultivo cultivo = cultivoOpt.get();
        Integer humedadMax = cultivo.getHumedadMaxOptima();

        if (humedadMax != null && data.humedad() >= humedadMax) {
            synchronized (this) {
                if (!riegoAutomaticoActivo) {
                    return;
                }
                if (apagadoProgramado != null) {
                    apagadoProgramado.cancel(false);
                    apagadoProgramado = null;
                }
                controlRiego.enviarComando(COMANDO_OFF);
                eventoRiegoService.completarRiego(data);
                riegoAutomaticoActivo = false;
            }
            eventPublisher.publishEvent(new RiegoFinalizadoEvent("humedad_maxima", data));
            System.out.println("[AUTO-RIEGO] Maximo (" + humedadMax + "%) alcanzado. Bomba apagada (2s).");
        }
    }

    @Override
    @Scheduled(cron = "0 * * * * *")
    public synchronized void evaluarRiegoAutomatico() {
        if (obtenerModoActual() != ModoOperacion.AUTOMATICO) {
            return;
        }

        Optional<Cultivo> cultivoActivo = obtenerCultivoAutomaticoSeleccionado();
        SensorData ultimoDato = esp32MqttSensor.getUltimoDato();

        if (cultivoActivo.isEmpty() || ultimoDato == null) {
            return;
        }

        Cultivo cultivo = cultivoActivo.get();

        if (riegoAutomaticoActivo) {
            Integer humedadMax = cultivo.getHumedadMaxOptima();
            int humedadActual = ultimoDato.humedad();
            System.out.println("[AUTO-RIEGO] Bomba encendida | Humedad actual: " + humedadActual + "% | Maximo configurado: " + humedadMax + "%");
            if (humedadMax != null && humedadActual >= humedadMax) {
                if (apagadoProgramado != null) {
                    apagadoProgramado.cancel(false);
                    apagadoProgramado = null;
                }
                controlRiego.enviarComando(COMANDO_OFF);
                eventoRiegoService.completarRiego(ultimoDato);
                riegoAutomaticoActivo = false;
                eventPublisher.publishEvent(new RiegoFinalizadoEvent("humedad_maxima", ultimoDato));
                System.out.println("[AUTO-RIEGO] Humedad maxima (" + humedadMax + "%) alcanzada. Bomba apagada.");
            }
            return;
        }

        Optional<LocalTime> horaProgramada = obtenerHoraRiegoProgramada();

        if (horaProgramada.isEmpty()) {
            return;
        }

        if (!esHoraProgramada(horaProgramada.get())) {
            return;
        }

        if (ultimoDato.humedad() >= cultivo.getHumedadMaxOptima()) {
            ultimaEjecucionProgramada = LocalDate.now();
            System.out.println("[AUTO-RIEGO] Humedad (" + ultimoDato.humedad() + "%) ya en o sobre el maximo (" + cultivo.getHumedadMaxOptima() + "%). Riego omitido.");
            return;
        }

        iniciarRiegoProgramado(cultivo);
    }

    private boolean esHoraProgramada(LocalTime horaProgramada) {
        LocalTime ahora = LocalTime.now();
        LocalDate hoy = LocalDate.now();

        return ahora.getHour() == horaProgramada.getHour()
                && ahora.getMinute() == horaProgramada.getMinute()
                && !hoy.equals(ultimaEjecucionProgramada);
    }

    private void iniciarRiegoProgramado(Cultivo cultivo) {
        Integer duracionMinutos = cultivo.getDuracionRiegoMinutos();

        if (duracionMinutos == null || duracionMinutos <= 0) {
            ultimaEjecucionProgramada = LocalDate.now();
            System.err.println("[AUTO-RIEGO] Perfil sin duracion valida. Riego omitido para " + cultivo.getNombre() + ".");
            return;
        }

        controlRiego.enviarComando(COMANDO_ON);
        eventoRiegoService.registrarInicio(cultivo.getId(), ModoRiego.AUTOMATICO, esp32MqttSensor.getUltimoDato());
        riegoAutomaticoActivo = true;
        ultimaEjecucionProgramada = LocalDate.now();

        Instant apagadoEn = Instant.now().plusSeconds(duracionMinutos * 60L);
        apagadoProgramado = taskScheduler.schedule(this::apagarRiegoProgramado, apagadoEn);

        System.out.println(
                "[AUTO-RIEGO] Bomba encendida para "
                        + cultivo.getNombre()
                        + " durante "
                        + duracionMinutos
                        + " minuto(s)."
        );
    }

    private synchronized void apagarRiegoProgramado() {
        if (!riegoAutomaticoActivo) {
            return;
        }

        controlRiego.enviarComando(COMANDO_OFF);
        SensorData ultimoDato = esp32MqttSensor.getUltimoDato();
        eventoRiegoService.completarRiego(ultimoDato);
        riegoAutomaticoActivo = false;
        apagadoProgramado = null;
        eventPublisher.publishEvent(new RiegoFinalizadoEvent("duracion_cumplida", ultimoDato));

        System.out.println("[AUTO-RIEGO] Duracion configurada cumplida. Bomba apagada automaticamente.");
    }

    @Override
    public synchronized void resetearProgramacion() {
        ultimaEjecucionProgramada = null;
    }

    @Override
    public synchronized void detenerRiegoActivo() {
        if (apagadoProgramado != null) {
            apagadoProgramado.cancel(false);
            apagadoProgramado = null;
        }
        riegoAutomaticoActivo = false;
        System.out.println("[AUTO-RIEGO] Riego activo cancelado por detencion manual.");
    }

    private ModoOperacion obtenerModoActual() {
        return obtenerConfiguracion()
                .map(ConfiguracionRiego::getModoOperacion)
                .orElse(ModoOperacion.MANUAL);
    }

    private Optional<Cultivo> obtenerCultivoAutomaticoSeleccionado() {
        return obtenerConfiguracion()
                .map(ConfiguracionRiego::getCultivoActivo);
    }

    private Optional<LocalTime> obtenerHoraRiegoProgramada() {
        return obtenerConfiguracion()
                .map(ConfiguracionRiego::getHoraRiegoProgramada);
    }

    private Optional<ConfiguracionRiego> obtenerConfiguracion() {
        return configuracionRiegoRepository.findById(CONFIG_ID);
    }
}
