package sri.project.sri_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SriProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(SriProjectApplication.class, args);
	}

}
    