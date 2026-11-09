package org.springframework.samples.petclinic.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerTest {

    @Test
    void unNouveauProprietaireSansIdEstConsidereCommeNouveau() {
        // Arrange : je cree un proprietaire, sans lui donner d'id
        Owner owner = new Owner();

        // Act : j'appelle la methode a tester
        boolean resultat = owner.isNew();

        // Assert : je verifie que le resultat est celui attendu
        assertThat(resultat).isTrue();
    }

    @Test
    void unProprietaireAvecUnIdNestPlusNouveau() {
        // Arrange : je cree un proprietaire et je lui donne un id
        Owner owner = new Owner();
        owner.setId(1);

        // Act
        boolean resultat = owner.isNew();

        // Assert
        assertThat(resultat).isFalse();
    }
}
