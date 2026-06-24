package sri.project.sri_project.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import sri.project.sri_project.dto.AlertaRiegoResponse;
import sri.project.sri_project.event.RiegoFinalizadoEvent;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AlertaRiegoService {

    private final AtomicLong secuencia = new AtomicLong();
    private volatile AlertaRiegoResponse ultimaAlerta;

    @EventListener
    public void registrarFinalizacion(RiegoFinalizadoEvent event) {
        if ("humedad_maxima".equals(event.getMotivo())) {
            ultimaAlerta = crear(
                    "warning",
                    "Riego detenido automaticamente",
                    "La humedad alcanzo el limite maximo configurado para el cultivo."
            );
        } else if ("tanque_vacio".equals(event.getMotivo())) {
            ultimaAlerta = crear(
                    "danger",
                    "Bomba detenida por seguridad",
                    "El tanque se quedo sin agua. Revise el nivel antes de reiniciar el riego."
            );
        }
    }

    public AlertaRiegoResponse obtenerUltimaAlerta() {
        return ultimaAlerta;
    }

    private AlertaRiegoResponse crear(String tipo, String titulo, String mensaje) {
        return new AlertaRiegoResponse(
                secuencia.incrementAndGet(),
                tipo,
                titulo,
                mensaje,
                LocalDateTime.now().toString()
        );
    }
}
