package sri.project.sri_project.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sri.project.sri_project.model.EventoRiego;
import sri.project.sri_project.model.enums.EstadoRiego;
import sri.project.sri_project.model.enums.ModoRiego;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRiegoRepository extends JpaRepository<EventoRiego,Long> {


// ---------------------------------------------------------
    // GRÁFICO 1: Dona/Pastel (Manual vs Automático del mes actual)
    // Usamos JPQL para contar directamente las entidades
    // ---------------------------------------------------------


    /*
    *
    * Para el grafico de Dona.
    * Comparativa de manual y automatico en el mes
    * */

    @Query("SELECT e.modoRiego, COUNT(e)" +
            " FROM EventoRiego e " +
            "WHERE FUNCTION('MONTH', e.fechaInicio) = FUNCTION('MONTH', CURRENT_DATE) " +
            "AND FUNCTION('YEAR', e.fechaInicio) = FUNCTION('YEAR', CURRENT_DATE) " +
            "GROUP BY e.modoRiego")
    List<Object[]> contarRiegosPorModoMesActual();




    // GRÁFICO 3: Barras (Consumo de tiempo de riego de los últimos 7 días)
    // Usamos Consulta Nativa para usar TIMESTAMPDIFF de MySQL

    @Query(value = "SELECT DATE(fecha_inicio) as fecha, SUM(TIMESTAMPDIFF(SECOND, fecha_inicio, fecha_fin)) as duracion_segundos " +
            "FROM eventos_riego " +
            "WHERE fecha_inicio >= NOW() - INTERVAL 7 DAY AND estado = 'COMPLETADO' " +
            "GROUP BY DATE(fecha_inicio) " +
            "ORDER BY fecha", nativeQuery = true)
    List<Object[]> obtenerDuracionDiariaUltimos7Dias();

    @Query(value = "SELECT DATE_FORMAT(e.fecha_inicio, '%Y-%m-%d') AS fecha, " +
            "COALESCE(c.nombre, 'Pruebas / Mantenimiento') AS cultivo, " +
            "DATE_FORMAT(e.fecha_inicio, '%H:%i:%s') AS hora_inicio, " +
            "DATE_FORMAT(e.fecha_fin, '%H:%i:%s') AS hora_fin, " +
            "ROUND((TIMESTAMPDIFF(SECOND, e.fecha_inicio, e.fecha_fin) / 60.0) * 1.0, 2) AS litros_consumidos " +
            "FROM eventos_riego e " +
            "LEFT JOIN perfiles_cultivo c ON c.id = e.cultivo_id " +
            "WHERE e.estado = 'COMPLETADO' AND e.fecha_fin IS NOT NULL " +
            "AND (:cultivoId IS NULL OR e.cultivo_id = :cultivoId) " +
            "AND (:soloMantenimiento = 0 OR e.cultivo_id IS NULL) " +
            "ORDER BY e.fecha_inicio DESC", nativeQuery = true)
    List<Object[]> obtenerConsumoAguaDetalle(@Param("cultivoId") Integer cultivoId,
                                              @Param("soloMantenimiento") Integer soloMantenimiento);

    long countByEstado(EstadoRiego estado);

    long countByModoRiegoAndFechaInicioBetween(ModoRiego modoRiego, LocalDateTime inicio, LocalDateTime fin);

    long countByModoRiegoAndCultivo_IdAndFechaInicioBetween(ModoRiego modoRiego,
                                                            Integer cultivoId,
                                                            LocalDateTime inicio,
                                                            LocalDateTime fin);

    long countByModoRiegoAndCultivoIsNullAndFechaInicioBetween(ModoRiego modoRiego,
                                                               LocalDateTime inicio,
                                                               LocalDateTime fin);

    Optional<EventoRiego> findFirstByEstadoOrderByFechaInicioDesc(EstadoRiego estado);

    EventoRiego findTopByOrderByFechaInicioDesc();

    @Query(value = "SELECT COALESCE(AVG(humedad_suelo_final - humedad_suelo_inicial), 0) " +
            "FROM eventos_riego " +
            "WHERE estado = 'COMPLETADO' AND humedad_suelo_final IS NOT NULL", nativeQuery = true)
    Double obtenerPromedioHumedadGanada();



}
