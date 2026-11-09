package org.springframework.samples.petclinic.rest.controller.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requete {@code POST /api/auth/login}.
 */
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password) {
}
