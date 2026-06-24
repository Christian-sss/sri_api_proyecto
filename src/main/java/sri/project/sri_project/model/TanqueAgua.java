/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sri.project.sri_project.model;


import org.springframework.stereotype.Component;
import sri.project.sri_project.model.enums.EstadoSistema;

@Component
public class TanqueAgua {
    private int humedad;
    private double distancia;
    private boolean bombaActiva;

    public static final int HUMEDAD_MINIMA = 30;
    private static final double UMBRAL_LLENO = 7.0;
    private static final double UMBRAL_VACIO = 18.0;

    private EstadoSistema estadoActual = EstadoSistema.ESPERA;

    public void actualizarDatos(int humedad, double distancia) {
        this.humedad = humedad;
        this.distancia = distancia;
    }



    public boolean hayAgua() {
        return distancia < UMBRAL_VACIO;
    }








    public String estadoTanque() {
        if (distancia <= UMBRAL_LLENO) {
            return "LLENO";
        } else if (distancia < UMBRAL_VACIO) {
            return "MEDIO";
        } else {
            return "VACIO";
        }
    }




    public void activarRiego() {

        if (hayAgua() && humedad < HUMEDAD_MINIMA) {
            activarBomba();
            estadoActual = EstadoSistema.REGANDO;
        }
    }
    public void detenerRiego() {
        bombaActiva = false;
        estadoActual = EstadoSistema.ESPERA;
    }



    public EstadoSistema getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(EstadoSistema estadoActual) {
        this.estadoActual = estadoActual;
    }

    public void activarBomba() {
        this.bombaActiva = true;
    }

    public void desactivarBomba() {
        this.bombaActiva = false;
    }

    public boolean isBombaActiva() {
        return bombaActiva;
    }

    public int getHumedad() {
        return humedad;
    }

    public double getDistancia() {
        return distancia;
    }

}
