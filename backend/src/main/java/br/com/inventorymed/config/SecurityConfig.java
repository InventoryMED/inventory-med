package br.com.inventorymed.config;

import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.SystemUserRoleRepository;
import br.com.inventorymed.security.AppUserDetailsService;
import br.com.inventorymed.security.SecurityErrorWriter;
import br.com.inventorymed.security.SessionAuthorizationFilter;
import br.com.inventorymed.security.SessionAuthorizationService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRepository csrfTokenRepository,
        SecurityErrorWriter securityErrorWriter,
        SessionAuthorizationService sessionAuthorizationService
    ) throws Exception {
        return http
            .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
            .cors(Customizer.withDefaults())
            .securityContext(context ->
                context
                    .securityContextRepository(securityContextRepository)
                    .requireExplicitSave(true)
            )
            .sessionManagement(session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(fixation -> fixation.changeSessionId())
            )
            .addFilterAfter(
                new SessionAuthorizationFilter(
                    sessionAuthorizationService,
                    securityContextRepository,
                    securityErrorWriter
                ),
                SecurityContextHolderFilter.class
            )
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/auth/csrf")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                        securityErrorWriter.write(
                            request,
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "AUTHENTICATION_REQUIRED",
                            "Autenticação necessária"
                        )
                    )
                    .accessDeniedHandler((request, response, exception) ->
                        securityErrorWriter.write(
                            request,
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "ACCESS_DENIED",
                            "Acesso negado"
                        )
                    )
            )
            .logout(logout ->
                logout
                    .logoutUrl("/auth/logout")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("INVENTORYMED_SESSION", "XSRF-TOKEN")
                    .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)
                    )
            )
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
        AppUserRepository userRepository,
        SystemUserRoleRepository systemRoleRepository,
        PasswordEncoder passwordEncoder
    ) {
        AppUserDetailsService userDetailsService = new AppUserDetailsService(
            userRepository,
            systemRoleRepository
        );
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CookieSerializer sessionCookieSerializer(
        @Value("${server.servlet.session.cookie.secure}") boolean secure
    ) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("INVENTORYMED_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite("Lax");
        return serializer;
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository(
        @Value("${server.servlet.session.cookie.secure}") boolean secure
    ) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie ->
            cookie.path("/").sameSite("Lax").secure(secure)
        );
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${inventory.security.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList()
        );
        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
            Arrays.asList("Content-Type", "X-XSRF-TOKEN", "X-Requested-With")
        );
        configuration.setExposedHeaders(Arrays.asList("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
