package sri.project.sri_project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/dashboard/**",
                        "/api/chat/**",
                        "/api/sensor/**",
                        "/api/riego/**",
                        "/api/mqtt/**",
                        "/api/cultivos/**",
                        "/api/estadisticas/**",
                        "/api/reportes/**"
                ).excludePathPatterns("/api/mqtt", "/api/mqtt/**");
    }
}
