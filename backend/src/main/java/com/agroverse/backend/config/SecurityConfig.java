package com.agroverse.backend.config;

import com.agroverse.backend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Value("${agroverse.cors.allowed-origins:http://localhost:3001,http://127.0.0.1:3001}")
    private String allowedOrigins;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Unauthorized: Authentication token is missing, invalid, or expired.\",\"status\":401}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Forbidden: You do not have permission to access this resource.\",\"status\":403}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**", "/api/users/register", "/api/users/login", "/api/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/uploads/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/weather/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/market/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/data/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/schemes", "/api/schemes/deadlines", "/api/schemes/search", "/api/schemes/filter", "/api/schemes/{id:[0-9]+}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/marketplace/products", "/api/marketplace/summary", "/api/marketplace/listings").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/community/posts", "/api/community/snapshot").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/community/posts/*/like", "/api/community/posts/{id}/like").permitAll()
                .requestMatchers("/api/chat", "/api/chat-v1").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Administrative government scheme management
                .requestMatchers(HttpMethod.POST, "/api/schemes").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/schemes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/schemes/**").hasRole("ADMIN")

                // Protected farmer actions
                .requestMatchers("/api/crop-detection/**").authenticated()
                .requestMatchers("/api/profile/**").authenticated()
                .requestMatchers("/api/home/**").authenticated()
                .requestMatchers("/api/crops/**").authenticated()
                .requestMatchers("/api/tasks/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/marketplace/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/marketplace/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/community/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/community/**").authenticated()
                .requestMatchers("/api/schemes/*/apply-click", "/api/schemes/applications/**").authenticated()

                // Default rule
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
