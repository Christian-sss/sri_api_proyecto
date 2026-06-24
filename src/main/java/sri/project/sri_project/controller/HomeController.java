package sri.project.sri_project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping({"/", "/api"})
    public Map<String, Object> inicio() {
        return Map.of(
                "nombre", "SRI API",
                "estado", "OK",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
