package org.springframework.samples.petclinic.security;

import org.springframework.stereotype.Component;

/**
 * Les quatre roles de l'application, du plus puissant au plus limite.
 * <ul>
 *     <li>{@link #ADMIN} : le gerant, peut tout faire (y compris gerer les utilisateurs et le referentiel)</li>
 *     <li>{@link #RECEPTIONIST} : la secretaire, gere proprietaires, animaux et visites</li>
 *     <li>{@link #VET} : le veterinaire, consulte proprietaires et animaux, cree et modifie des visites</li>
 *     <li>{@link #OWNER} : le client, consulte uniquement ses propres donnees</li>
 * </ul>
 * Utilise dans les annotations {@code @PreAuthorize} sous le nom {@code @roles}.
 */
@Component
public class Roles {

    public final String ADMIN = "ROLE_ADMIN";
    public final String RECEPTIONIST = "ROLE_RECEPTIONIST";
    public final String VET = "ROLE_VET";
    public final String OWNER = "ROLE_OWNER";
}
