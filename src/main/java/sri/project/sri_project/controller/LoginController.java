package sri.project.sri_project.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sri.project.sri_project.model.User;
import sri.project.sri_project.service.UsuarioService;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class LoginController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> ingresar(@RequestBody AuthRequest request, HttpSession session) {
        if (request == null || esBlanco(request.username()) || esBlanco(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "username y password son obligatorios."));
        }

        try {
            User usuario = usuarioService.ejecutar(request.username().trim(), request.password().trim());
            session.setAttribute("usuarioLogueado", usuario);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Login correcto.",
                    "usuario", toUsuarioResponse(usuario)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario o contrasena incorrectos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody AuthRequest request) {
        if (request == null || esBlanco(request.username()) || esBlanco(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "username y password son obligatorios."));
        }

        try {
            User usuario = usuarioService.registrar(request.username().trim(), request.password().trim());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Registro exitoso.",
                    "usuario", toUsuarioResponse(usuario)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocurrio un error en el registro."));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> ingresarConGoogle(@RequestBody GoogleAuthRequest request, HttpSession session) {
        if (request == null || esBlanco(request.credential())) {
            return ResponseEntity.badRequest().body(Map.of("error", "credential es obligatorio."));
        }

        try {
            User usuario = usuarioService.autenticarConGoogle(request.credential());
            session.setAttribute("usuarioLogueado", usuario);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Login con Google correcto.",
                    "usuario", toUsuarioResponse(usuario)
            ));
        } catch (Exception e) {
            String mensajeDetalle = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Error autenticando con Google: " + mensajeDetalle));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> usuarioActual(HttpSession session) {
        User usuario = (User) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No hay una sesion activa."));
        }

        return ResponseEntity.ok(Map.of("usuario", toUsuarioResponse(usuario)));
    }

    @PostMapping("/logout")
    public Map<String, String> salir(HttpSession session) {
        session.invalidate();
        return Map.of("mensaje", "Sesion cerrada correctamente.");
    }

    private Map<String, Object> toUsuarioResponse(User usuario) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", usuario.getId());
        response.put("nombre", usuario.getNombre());
        response.put("email", usuario.getEmail());
        response.put("pictureUrl", usuario.getPictureUrl());
        response.put("fechaCreacion", usuario.getFechaCreacion());
        return response;
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }

    private record AuthRequest(String username, String password) {
    }

    private record GoogleAuthRequest(String credential) {
    }
}
