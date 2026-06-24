/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package sri.project.sri_project.service;

import sri.project.sri_project.model.User;

/**
 *
 * @author Usuario
 */


public interface UsuarioService {
    
    User ejecutar(String username, String passwordIngresada);
    User autenticarConGoogle(String idTokenString);
    User registrar(String email, String password);
}
