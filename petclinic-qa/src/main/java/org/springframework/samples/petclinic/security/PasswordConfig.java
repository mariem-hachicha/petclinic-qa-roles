package org.springframework.samples.petclinic.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Encodeur de mots de passe BCrypt.
 * <p>
 * Declare a part de {@link JwtSecurityConfig} car il doit exister meme quand la securite est desactivee :
 * {@code UserServiceImpl} l'utilise pour hacher le mot de passe des utilisateurs crees via l'API.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
