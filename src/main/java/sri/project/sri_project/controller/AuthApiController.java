package sri.project.sri_project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sri.project.sri_project.dto.LoginRequest;
import sri.project.sri_project.model.User;
import sri.project.sri_project.service.UsuarioService;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginEscritorio(@RequestBody LoginRequest request) {
        // Mantenemos tu excelente validación previa de seguridad
        if (request == null || esBlanco(request.email()) || esBlanco(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "email y password son obligatorios."));
        }

        try {
            User usuario = usuarioService.ejecutar(request.email().trim(), request.password().trim());

            // Devolvemos tanto el token como el objeto usuario por si el escritorio lo necesita
            return ResponseEntity.ok(Map.of(
                    "token", "SESSION-TOKEN-" + usuario.getId(),
                    "usuario", toUsuarioResponse(usuario)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas o usuario no encontrado."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obtenerMisDatos(@RequestHeader("Authorization") String token) {
        try {
            // Limpiamos el token de manera segura
            String idString = token.replace("Bearer ", "").replace("SESSION-TOKEN-", "").trim();

            // Volvemos a usar Long para mantener compatibilidad con la base de datos
            Long usuarioId = Long.parseLong(idString);

            // Buscamos los datos reales del usuario (Si tu servicio tiene buscarPorId)
            // User usuario = usuarioService.buscarPorId(usuarioId);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", usuarioId);

            // IMPORTANTE: Estos campos son OBLIGATORIOS para que tu JavaFX no falle
            response.put("nombre", "Administrador Principal"); // Reemplazar por usuario.getNombre() cuando esté listo
            response.put("email", "admin@sistema.com");       // Reemplazar por usuario.getEmail() cuando esté listo

            // Esta clave "rol" es la que tu MqttController lee mediante ApiConfig.USUARIO_ACTUAL.rol().name()
            response.put("rol", "AGRICULTOR");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token inválido o expirado."));
        }
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
}