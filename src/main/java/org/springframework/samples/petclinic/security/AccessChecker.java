package org.springframework.samples.petclinic.security;

import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Controle d'acces au niveau des donnees pour le role OWNER (client).
 * <p>
 * Un client ne doit voir que SES donnees : sa fiche proprietaire, ses animaux et leurs visites.
 * L'identifiant du proprietaire lie au compte est lu dans le jeton JWT (revendication {@code ownerId}).
 * <p>
 * Utilise dans les annotations {@code @PreAuthorize} sous le nom {@code @access}, par exemple :
 * {@code @access.isOwner(#ownerId)}.
 * <p>
 * Si la ressource n'existe pas, les methodes renvoient {@code false} : le client obtient 403 et non 404,
 * pour ne pas lui reveler quels identifiants existent.
 */
@Component("access")
public class AccessChecker {

    private final ClinicService clinicService;

    public AccessChecker(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    /** Le proprietaire demande est-il celui du compte connecte ? */
    public boolean isOwner(Integer ownerId) {
        Integer current = currentOwnerId();
        return current != null && ownerId != null && current.equals(ownerId);
    }

    /** L'animal appartient-il au proprietaire du compte connecte ? */
    public boolean ownsPet(Integer petId) {
        if (petId == null) {
            return false;
        }
        Pet pet = clinicService.findPetById(petId);
        return pet != null && pet.getOwner() != null && isOwner(pet.getOwner().getId());
    }

    /** La visite concerne-t-elle un animal du proprietaire du compte connecte ? */
    public boolean ownsVisit(Integer visitId) {
        if (visitId == null) {
            return false;
        }
        Visit visit = clinicService.findVisitById(visitId);
        return visit != null && visit.getPet() != null && ownsPet(visit.getPet().getId());
    }

    /** Lit la revendication "ownerId" du jeton JWT de la requete en cours ; null si absente. */
    private Integer currentOwnerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object claim = jwtAuthentication.getToken().getClaims().get(JwtSecurityConfig.OWNER_ID_CLAIM);
            if (claim instanceof Number number) {
                return number.intValue();
            }
        }
        return null;
    }
}
