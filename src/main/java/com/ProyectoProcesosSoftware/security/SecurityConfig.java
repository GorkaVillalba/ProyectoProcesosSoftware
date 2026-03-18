package com.ProyectoProcesosSoftware.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Importante para el hasheo
import org.springframework.security.crypto.password.PasswordEncoder; // Interfaz del encriptador

@Configuration
public class SecurityConfig {

    // Configuración de acceso: Permite que podamos usar la API sin login por ahora
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Desactivamos CSRF para poder hacer POST desde Postman
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // Permitimos todas las rutas
        return http.build();
    }

    // Definición del Encriptador: Para que UsuarioService pueda usar BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}