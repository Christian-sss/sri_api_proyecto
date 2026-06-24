package sri.project.sri_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sri.project.sri_project.model.LecturaSensor;

import java.util.List;

@Repository
public interface LecturaSensorRepository extends JpaRepository<LecturaSensor, Long> {

    List<LecturaSensor> findTop20ByOrderByFechaLecturaDesc();

    List<LecturaSensor> findTop20ByCultivoIdOrderByFechaLecturaDesc(Integer cultivoId);

    List<LecturaSensor> findTop20ByCultivoIdIsNullOrderByFechaLecturaDesc();
}
