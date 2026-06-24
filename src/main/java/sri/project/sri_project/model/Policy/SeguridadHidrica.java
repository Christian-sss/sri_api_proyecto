/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sri.project.sri_project.model.Policy;


/**
 *
 * @author Usuario
 */


import sri.project.sri_project.model.TanqueAgua;
import sri.project.sri_project.model.enums.EstadoSistema;
import org.springframework.stereotype.Component;


@Component
public class SeguridadHidrica  {

    public void evaluarEstado(TanqueAgua tanque) {

        if (!tanque.hayAgua()) {
            tanque.setEstadoActual(EstadoSistema.BLOQUEADO_SIN_AGUA);

            tanque.desactivarBomba();
            return;
        }

        if (tanque.isBombaActiva()) {
            tanque.setEstadoActual(EstadoSistema.REGANDO);
            return;
        }


        tanque.setEstadoActual(EstadoSistema.ESPERA);
    }


}
