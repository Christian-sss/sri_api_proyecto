/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sri.project.sri_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sri.project.sri_project.model.User;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<User,Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrNombre(String email, String nombre);

}
