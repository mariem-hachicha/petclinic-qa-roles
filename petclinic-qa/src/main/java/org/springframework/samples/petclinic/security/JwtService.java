package org.springframework.samples.petclinic.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.samples.petclinic.rest.controller.auth.TokenResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Cree les jetons JWT apres un login reussi.
 * <p>
 * Contenu d'un jeton : emetteur ({@code iss}), utilisateur ({@code sub}), date de creation ({@code iat}),
 * date d'expiration ({@code exp}), roles ({@code roles}) et, pour un client (role OWNER),
 * l'identifiant du proprietaire lie au compte ({@code ownerId}).
 */
@Service
@ConditionalOnProperty(name = "petclinic.security.enable", havingValue = "true")
public class JwtService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder jwtEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final long expirationMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      JdbcTemplate jdbcTemplate,
                      @Value("${petclinic.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.expirationMinutes = expirationMinutes;
    }

    public TokenResponse generateToken(Authentication authentication) {
        Instant now = Instant.now();
        // On ne garde que les roles metier : Spring Security peut ajouter d'autres autorites techniques
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority != null && authority.startsWith(ROLE_PREFIX))
            .sorted()
            .toList();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .issuer(JwtSecurityConfig.ISSUER)
            .subject(authentication.getName())
            .issuedAt(now)
            .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
            .claim(JwtSecurityConfig.ROLES_CLAIM, roles);
        Integer ownerId = findOwnerId(authentication.getName());
        if (ownerId != null) {
            claims.claim(JwtSecurityConfig.OWNER_ID_CLAIM, ownerId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenResponse(token, "Bearer", expirationMinutes * 60);
    }

    /** Identifiant du proprietaire lie au compte (colonne users.owner_id), ou null pour le personnel. */
    private Integer findOwnerId(String username) {
        ResultSetExtractor<Integer> firstOwnerId = rs -> rs.next() ? rs.getObject("owner_id", Integer.class) : null;
        return jdbcTemplate.query("select owner_id from users where username = ?", firstOwnerId, username);
    }
}
