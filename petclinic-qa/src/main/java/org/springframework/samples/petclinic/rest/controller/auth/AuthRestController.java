package org.springframework.samples.petclinic.rest.controller.auth;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entree de l'authentification : echange identifiant + mot de passe contre un jeton JWT.
 * <ul>
 *     <li>200 + jeton si les identifiants sont corrects</li>
 *     <li>400 si un champ est vide ou absent</li>
 *     <li>401 si l'identifiant ou le mot de passe est faux, ou si le compte est desactive</li>
 * </ul>
 */
@RestController
@CrossOrigin(exposedHeaders = "content-type")
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "petclinic.security.enable", havingValue = "true")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthRestController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return ResponseEntity.ok(jwtService.generateToken(authentication));
    }
}
