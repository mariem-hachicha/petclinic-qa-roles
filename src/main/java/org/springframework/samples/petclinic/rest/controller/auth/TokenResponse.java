package org.springframework.samples.petclinic.rest.controller.auth;

/**
 * Reponse d'un login reussi.
 *
 * @param accessToken le jeton JWT a envoyer dans l'en-tete {@code Authorization: Bearer <jeton>}
 * @param tokenType   toujours {@code Bearer}
 * @param expiresIn   duree de validite du jeton, en secondes
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
