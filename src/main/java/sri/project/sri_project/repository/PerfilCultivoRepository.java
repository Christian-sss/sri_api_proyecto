package sri.project.sri_project.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sri.project.sri_project.model.Cultivo;

import java.util.List;

@Repository
public interface PerfilCultivoRepository extends JpaRepository<Cultivo,Integer> {

    List<Cultivo> findByActivoTrue();

    List<Cultivo> findByActivoFalse();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

}
