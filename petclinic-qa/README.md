# PetClinic REST — système sous test (Projet de synthèse, Sujet 12)

Version simplifiée de [spring-petclinic-rest](https://github.com/spring-petclinic/spring-petclinic-rest)
(commit d'origine `5180cc9bd0163393c4e628bedc1bc4c27cfaab1b`, licence Apache 2.0, voir `LICENSE.txt`),
utilisée comme système sous test pour un pipeline QA piloté par IA.

## Modifications par rapport au projet d'origine

| Modification | Raison |
|---|---|
| Suppression des couches `repository/jdbc` et `repository/jpa` | Implémentations alternatives jamais chargées ; seule Spring Data JPA est conservée |
| Suppression de `util/` (`CallMonitoringAspect`, `EntityUtils`) | Aspect non enregistré ; utilitaire utilisé uniquement par la couche JDBC |
| Suppression de `BindingErrorsResponse` | Code mort : son résultat n'était jamais renvoyé au client |
| Suppression des configurations MySQL et HSQLDB | PostgreSQL pour l'application, H2 pour les tests |
| Suppression des tests d'origine (JUnit, Postman, JMeter) et de la CI d'origine | Les tests sont réécrits dans le cadre du projet |
| Remplacement de HTTP Basic par **JWT** | Authentification par jeton : `POST /api/auth/login` |
| Gestion explicite des erreurs 401 et 403 | Avant, ces erreurs étaient converties en 500 par le gestionnaire générique |
| Nouveau modèle de **4 rôles** (`ADMIN`, `RECEPTIONIST`, `VET`, `OWNER`) | Remplace `OWNER_ADMIN` / `VET_ADMIN` / `ADMIN`, qui ne correspondaient pas aux métiers d'une clinique |
| Accès aux données limité pour les clients (`OWNER`) | Un client ne voit que sa fiche, ses animaux et leurs visites (classe `AccessChecker`) |
| Colonne `users.owner_id` | Relie un compte client à sa fiche propriétaire |
| Hachage BCrypt du mot de passe à la création d'un utilisateur (`UserServiceImpl`) | Correction d'un bug : le mot de passe était stocké en clair, l'utilisateur ne pouvait jamais se connecter |

## Prérequis

- Java 17 ou plus
- Docker Desktop (pour PostgreSQL)

PostgreSQL est exposé sur le port **5433** (et non 5432), pour éviter le conflit avec un PostgreSQL déjà installé sur la machine.

## Réinitialiser la base PostgreSQL

Les scripts `schema.sql` / `data.sql` ne recréent pas une table qui existe déjà. Après une modification
du schéma, repartir d'une base vide :

```cmd
docker compose down -v
docker compose up -d
```

## Lancer l'application (Windows, CMD)

```cmd
docker compose up -d
mvnw.cmd spring-boot:run
```

- API : `http://localhost:9966/petclinic/api/...`
- Swagger UI : `http://localhost:9966/petclinic/swagger-ui.html`

## S'authentifier

Utilisateurs de démonstration (le mot de passe est identique au nom d'utilisateur) :

| Utilisateur | Mot de passe | Rôle | Fiche propriétaire liée |
|---|---|---|---|
| `admin` | `admin` | `ROLE_ADMIN` | — |
| `reception` | `reception` | `ROLE_RECEPTIONIST` | — |
| `vet` | `vet` | `ROLE_VET` | — |
| `george` | `george` | `ROLE_OWNER` | propriétaire n°1 (George Franklin) |
| `betty` | `betty` | `ROLE_OWNER` | propriétaire n°2 (Betty Davis) |

```cmd
curl -X POST http://localhost:9966/petclinic/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin\"}"
```

Réponse :

```json
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600 }
```

Puis envoyer le jeton à chaque requête :

```cmd
curl http://localhost:9966/petclinic/api/owners -H "Authorization: Bearer <accessToken>"
```

Dans Swagger UI : appeler `/api/auth/login`, copier `accessToken`, cliquer sur **Authorize** et le coller.

Contenu du jeton : `iss`, `sub` (utilisateur), `iat`, `exp`, `roles` et, pour un client, `ownerId`.

| Situation | Code HTTP |
|---|---|
| Login correct | 200 + jeton |
| Login avec champ vide | 400 |
| Mauvais identifiant / mot de passe, compte désactivé | 401 |
| Requête sans jeton, jeton expiré, modifié ou mal signé | 401 |
| Jeton valide mais rôle insuffisant | 403 |
| Client qui demande les données d'un autre client (ou un identifiant inexistant) | 403 |

## Matrice des droits

✔ = autorisé · « siennes » = uniquement les données du client connecté · ✘ = 403

| Opération | ADMIN | RECEPTIONIST | VET | OWNER |
|---|---|---|---|---|
| Lister propriétaires (v1 et v2) | ✔ | ✔ | ✔ | ✘ |
| Consulter un propriétaire / un de ses animaux | ✔ | ✔ | ✔ | siennes |
| Créer, modifier, supprimer un propriétaire | ✔ | ✔ | ✘ | ✘ |
| Ajouter ou modifier l'animal d'un propriétaire | ✔ | ✔ | ✘ | ✘ |
| Lister les animaux (v1 et v2) | ✔ | ✔ | ✔ | ✘ |
| Consulter un animal | ✔ | ✔ | ✔ | siennes |
| Modifier, supprimer un animal | ✔ | ✔ | ✘ | ✘ |
| Lister les visites | ✔ | ✔ | ✔ | ✘ |
| Consulter une visite | ✔ | ✔ | ✔ | siennes |
| Créer, modifier une visite | ✔ | ✔ | ✔ | ✘ |
| Supprimer une visite | ✔ | ✔ | ✘ | ✘ |
| Consulter vétérinaires, spécialités, types d'animaux | ✔ | ✔ | ✔ | ✘ |
| Créer, modifier, supprimer vétérinaires, spécialités, types | ✔ | ✘ | ✘ | ✘ |
| Créer un utilisateur | ✔ | ✘ | ✘ | ✘ |

Les règles sont écrites dans les annotations `@PreAuthorize` de chaque contrôleur ; le contrôle
« siennes » est fait par la classe `security/AccessChecker`.

Limite connue : l'API `POST /api/users` ne permet pas de relier un compte à une fiche propriétaire ;
les comptes clients sont créés dans `data.sql`.

Désactiver la sécurité : `petclinic.security.enable=false` dans `src/main/resources/application.properties`.

## Configuration

| Propriété | Défaut | Rôle |
|---|---|---|
| `petclinic.security.enable` | `true` | Active le JWT |
| `petclinic.jwt.secret` | variable `JWT_SECRET`, sinon une valeur de développement | Clé HMAC-SHA256 (32 caractères minimum) |
| `petclinic.jwt.expiration-minutes` | `60` | Durée de validité d'un jeton |

## Lancer les tests

```cmd
mvnw.cmd test
```

Les tests utilisent une base H2 en mémoire (`src/test/resources/application.properties`), sans Docker,
avec la sécurité désactivée par défaut.
