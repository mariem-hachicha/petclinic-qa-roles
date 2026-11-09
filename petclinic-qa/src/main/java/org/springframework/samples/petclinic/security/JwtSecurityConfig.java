package org.springframework.samples.petclinic.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de securite par jeton JWT.
 * <p>
 * Active uniquement quand {@code petclinic.security.enable=true}.
 * Le bean {@code PasswordEncoder} (BCrypt) est declare dans {@link PasswordConfig}.
 * <ol>
 *     <li>Le client appelle {@code POST /api/auth/login} avec son identifiant et son mot de passe.</li>
 *     <li>Le serveur verifie le mot de passe (BCrypt, tables {@code users} et {@code roles})
 *     et renvoie un jeton signe en HMAC-SHA256 contenant l'utilisateur et ses roles.</li>
 *     <li>Le client envoie ensuite ce jeton dans l'en-tete {@code Authorization: Bearer <jeton>}.</li>
 *     <li>Les annotations {@code @PreAuthorize} des controleurs controlent les roles.</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "petclinic.security.enable", havingValue = "true")
public class JwtSecurityConfig {

    /** Nom de la revendication (claim) du jeton qui contient les roles. */
    public static final String ROLES_CLAIM = "roles";

    /** Nom de la revendication qui contient l'identifiant du proprietaire lie au compte (role OWNER). */
    public static final String OWNER_ID_CLAIM = "ownerId";

    /** Emetteur ecrit dans chaque jeton et verifie a la reception. */
    public static final String ISSUER = "petclinic";

    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    /** Lit les utilisateurs et leurs roles dans la base, comme le faisait l'ancienne configuration HTTP Basic. */
    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery("select username, password, enabled from users where username = ?");
        manager.setAuthoritiesByUsernameQuery("select username, role from roles where username = ?");
        return manager;
    }

    /** Verifie identifiant + mot de passe au moment du login. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${petclinic.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                "petclinic.jwt.secret doit contenir au moins " + MIN_SECRET_LENGTH_BYTES + " octets pour HMAC-SHA256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Signe les jetons au moment du login. */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * Verifie chaque jeton recu : signature, algorithme, emetteur et expiration.
     * La tolerance d'horloge est mise a zero pour qu'un jeton expire exactement a sa date d'expiration
     * (par defaut Spring accepte 60 secondes de retard).
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(Duration.ZERO),
            new JwtIssuerValidator(ISSUER)));
        return decoder;
    }

    /** Transforme la revendication "roles" du jeton en autorites Spring Security (ex. ROLE_ADMIN). */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(ROLES_CLAIM);
        authoritiesConverter.setAuthorityPrefix(""); // les roles sont deja stockes avec le prefixe ROLE_
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                    "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
