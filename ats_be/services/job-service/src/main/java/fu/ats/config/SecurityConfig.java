package fu.ats.config;

import fu.ats.api.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableMethodSecurity
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http.csrf((csrf) -> {
                    csrf.disable();
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests((author) -> {
                    author.requestMatchers("/actuator/health").permitAll()
                            .anyRequest().authenticated();
                })
                // Return 401 (not the default 403) when an unauthenticated user hits a protected URL
                .exceptionHandling((ex) -> {
                    log.info("securityFilterChain - {}", ex.toString());
                    ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
                    ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.warn("Access denied for {} {}: {}",
                                request.getMethod(),
                                request.getRequestURI(),
                                accessDeniedException.getMessage());
                        // Do not call sendError here: it triggers an /error dispatch,
                        // which is protected and can overwrite 403 with 401.
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                    });
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();

    }
}
