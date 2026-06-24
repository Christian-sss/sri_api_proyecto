package sri.project.sri_project.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("Eres un asistente experto para un Sistema de Riego Inteligente. " +
                        "Responde de forma breve y concisa sobre el cuidado del agua y cultivos.")
                .build();
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> enviarMensaje(@RequestBody ChatRequest request) {
        String pregunta = request == null || request.mensaje() == null ? "" : request.mensaje().trim();
        if (pregunta.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Escribe una pregunta para consultar al asistente."));
        }

        try {
            String respuestaBot = this.chatClient.prompt()
                    .user(pregunta)
                    .call()
                    .content();

            return ResponseEntity.ok(Map.of(
                    "pregunta", pregunta,
                    "respuesta", respuestaBot
            ));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No fue posible responder en este momento. Intenta nuevamente."));
        }
    }

    private record ChatRequest(String mensaje) {
    }
}
