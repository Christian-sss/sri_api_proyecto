package sri.project.sri_project.service.serviceImpl;

import org.springframework.stereotype.Service;
import sri.project.sri_project.model.User;
import sri.project.sri_project.repository.UsuarioRepository;
import sri.project.sri_project.service.UsuarioService;
import sri.project.sri_project.service.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public UsuarioServiceImpl(UsuarioRepository userRepository, EmailService emailService, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.usuarioRepository = userRepository;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User registrar(String email, String password) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }
        

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setNombre(email.split("@")[0]);
        newUser.setPasswordHash(password);
        newUser.setPictureUrl("");
        User savedUser = usuarioRepository.save(newUser);
        
        try {
            jdbcTemplate.update("INSERT IGNORE INTO sisintupt.usuario_auth (CorreoU) VALUES (?)", email);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Aviso: No se pudo insertar en sisintupt.usuario_auth. Asegúrate de que la base de datos existe.");
        }
        

        new Thread(() -> {
            try {
                emailService.enviarCorreoRegistro(email, newUser.getNombre());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return savedUser;
    }

    @Override
    public User ejecutar(String email, String passwordIngresada) {
        User user = usuarioRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (!passwordIngresada.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Contraseña incorrecta.");
        }

        return user;
    }

    @Override
    public User autenticarConGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("571504675373-b3dthv13b7i5khpi4p4dvnq09icbfe4n.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                boolean isNewUser = !usuarioRepository.findByEmail(email).isPresent();
                
                User loggedInUser = usuarioRepository.findByEmail(email).map(user -> {
                    user.setPictureUrl(pictureUrl);
                    return usuarioRepository.save(user);
                }).orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setNombre(name);
                    newUser.setPasswordHash("");
                    newUser.setPictureUrl(pictureUrl);
                    return usuarioRepository.save(newUser);
                });
                

                try {
                    jdbcTemplate.update("INSERT IGNORE INTO sisintupt.usuario_auth (CorreoU) VALUES (?)", email);
                } catch (Exception e) {
                }
                
                new Thread(() -> {
                    try {
                        if (isNewUser) {
                            emailService.enviarCorreoRegistro(email, name);
                        } else {
                            emailService.enviarCorreoLogin(email, name);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        try {
                            java.nio.file.Files.writeString(java.nio.file.Path.of("mail_error.log"), "Mail Thread Error: " + ex.getMessage(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                        } catch (Exception ignored) {}
                    }
                }).start();
                
                return loggedInUser;
            } else {
                throw new IllegalArgumentException("Token de Google inválido.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error verificando token de Google", e);
        }
    }
}
