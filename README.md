# SunuAgri — Plateforme de la coopérative agricole

Cœur de la plateforme de la coopérative SunuAgri (cas fictif, Sénégal) : suivi des producteurs, des
parcelles et des cultures au fil des campagnes, réception des récoltes à l'entrepôt, vente aux acheteurs
et répartition de l'argent des ventes entre les producteurs.

Projet du **module 5.1 — DDD, architecture hexagonale et API Design**, réalisé en **Java 21 / Spring Boot 3.4**.

---

## 1. Livrables

| Livrable | Emplacement |
|---|---|
| Code source | [`src/main/java/sn/sunuagri`](src/main/java/sn/sunuagri) |
| Tests automatisés (268 tests, sans serveur ni base) | [`src/test/java/sn/sunuagri`](src/test/java/sn/sunuagri) |
| Contrat OpenAPI | [`docs/openapi.yaml`](docs/openapi.yaml) et Swagger UI en direct (`/swagger-ui.html`) |
| Glossaire | [`docs/glossaire.md`](docs/glossaire.md) |
| Document de conception (contextes, agrégats, règles par story, hypothèses) | [`docs/conception.md`](docs/conception.md) |
| Décisions d'architecture (6 ADR) | [`docs/adr/`](docs/adr) |
| Démonstration du scénario complet | [`docs/demo/scenario-complet.sh`](docs/demo/scenario-complet.sh) |

## 2. Lancer le projet

### Prérequis
- Java 21 ou plus récent, Maven 3.9+
- Accès Internet vers la base PostgreSQL **Neon** (déjà configurée dans `application.properties`)

### Démarrer l'application
```bash
mvn spring-boot:run
```
L'application démarre sur <http://localhost:8080>. Les tables sont créées automatiquement et les
paramètres de référence des cultures sont chargés au premier démarrage.

- **Swagger UI** : <http://localhost:8080/swagger-ui.html>
- **Contrat OpenAPI** : <http://localhost:8080/v3/api-docs.yaml>

Pour utiliser une autre base sans modifier le code, définissez les variables d'environnement
`DB_URL`, `DB_USERNAME` et `DB_PASSWORD` (elles remplacent les valeurs par défaut).

### S'identifier (identité simulée)
L'authentification est hors périmètre : l'utilisateur est simulé par deux en-têtes HTTP, à renseigner
dans Swagger via le bouton **Authorize** :

| En-tête | Valeurs |
|---|---|
| `X-Role` | `AGENT`, `PRODUCTEUR`, `ACHETEUR` ou `ADMINISTRATEUR` |
| `X-Utilisateur-Id` | obligatoire pour `PRODUCTEUR` (id du producteur) et `ACHETEUR` (ex. `ACH-001`) |

## 3. Lancer les tests

```bash
mvn test
```

Les tests n'utilisent **ni serveur web ni base de données** (exigence E1) : les cas d'usage sont testés
avec des adaptateurs en mémoire. Ils comprennent :

- les règles de chaque agrégat et de chaque story ;
- un test de la répartition des paiements sur 200 cas aléatoires (aucun franc perdu ni créé) ;
- un test d'architecture (ArchUnit) qui vérifie que le domaine et l'application ne dépendent ni de
  Spring, ni de JPA, ni de l'infrastructure.

## 4. Démonstration : du producteur au paiement dû

Application démarrée, dans un autre terminal (nécessite `curl` et `jq`) :

```bash
./docs/demo/scenario-complet.sh
```

Le script enchaîne : enregistrement de deux producteurs → parcelles → ouverture de campagne → cultures
d'arachide → semis → récoltes (dont une refusée comme suspecte) → livraisons à l'entrepôt (dont une
refusée car trop humide) → état du stock → prix du marché → publication d'une offre → commande d'un
acheteur → livraison de la commande avec consommation des lots (du plus ancien au plus récent),
commission de 5 % et paiements dus → SMS simulés → consultation de ses paiements par le producteur.

Résultat attendu de la livraison (600 kg à 350 XOF/kg) :

| | Montant |
|---|---|
| Montant de la commande | 210 000 XOF |
| Commission de la coopérative (5 %) | 10 500 XOF |
| Awa — 400 kg apportés | 133 000 XOF (à payer, SMS envoyé) |
| Modou — 200 kg apportés | 66 500 XOF (à payer, SMS envoyé) |

Les SMS simulés apparaissent dans le journal de l'application (`[SMS SIMULÉ]`).
Pour simuler un échec d'envoi, ajoutez le numéro du producteur à
`sunuagri.sms.simulation.numeros-en-echec` : la livraison reste valide et l'échec est tracé sur le
paiement (`notificationSms.statut = ECHEC`).

## 5. Stories réalisées

**Toutes les stories ont été réalisées** : les Indispensables, toutes les Importantes et les deux Bonus.

| Story | Priorité | Statut | Point d'entrée de l'API |
|---|---|---|---|
| US-01 Enregistrer un producteur | Indispensable | ✅ | `POST /api/v1/producteurs` |
| US-02 Déclarer une parcelle | Indispensable | ✅ | `POST /api/v1/producteurs/{id}/parcelles` |
| US-03 Consulter les parcelles d'un producteur | Importante | ✅ | `GET /api/v1/producteurs/{id}/parcelles[/{parcelleId}]` |
| US-04 Ouvrir et clôturer une campagne | Indispensable | ✅ | `POST /api/v1/campagnes`, `POST /api/v1/campagnes/{id}/cloture` |
| US-05 Déclarer une culture sur une parcelle | Indispensable | ✅ | `POST /api/v1/cultures` |
| US-06 Suivre l'avancement d'une culture | Importante | ✅ | `POST /api/v1/cultures/{id}/semis`, `POST /api/v1/cultures/{id}/recolte` |
| US-07 Déclarer une récolte | Indispensable | ✅ | `POST /api/v1/cultures/{id}/recolte` |
| US-08 Livrer une récolte à l'entrepôt | Indispensable | ✅ | `POST /api/v1/cultures/{id}/livraisons` |
| US-09 Consulter le stock | Importante | ✅ | `GET /api/v1/stocks?culture=&qualite=` |
| US-10 Publier une offre de vente | Indispensable | ✅ | `POST /api/v1/offres`, `POST /api/v1/offres/{id}/retrait` |
| US-11 Passer une commande | Indispensable | ✅ | `POST /api/v1/commandes` |
| US-12 Annuler une commande | Importante | ✅ | `POST /api/v1/commandes/{id}/annulation` |
| US-13 Livrer une commande et répartir le paiement | Indispensable | ✅ | `POST /api/v1/commandes/{id}/livraison` |
| US-14 Être prévenu par SMS d'un paiement | Importante | ✅ | (automatique à la livraison) `GET /api/v1/paiements/{id}` |
| US-15 Importer des producteurs en lot (CSV) | Importante | ✅ | `POST /api/v1/producteurs/imports` |
| US-16 Consulter l'historique de mes commandes | Importante | ✅ | `GET /api/v1/commandes?statut=&page=&taille=` |
| US-17 Accéder uniquement à ce qui me concerne | Indispensable | ✅ | (contrôlé sur toutes les ressources) |
| US-18 Connaître le prix de référence du marché | Bonus | ✅ | `GET /api/v1/prix-marche/{culture}` |
| US-19 Gérer les paramètres des cultures | Bonus | ✅ | `GET/PUT /api/v1/parametres-cultures/{culture}` |

Les règles détaillées de chaque story, leurs codes d'erreur et les hypothèses retenues sont dans
[`docs/conception.md`](docs/conception.md).

## 6. Exigences transversales

| Exigence | Comment elle est satisfaite |
|---|---|
| **E1** Règles testées vite, sans serveur ni base | Domaine et application en Java pur ; 268 tests avec adaptateurs en mémoire (≈ 2 s) |
| **E2** Base, SMS, prix remplaçables | Ports (`*Repository`, `EnvoiSms`, `FournisseurPrixMarche`) et adaptateurs ([ADR-001](docs/adr/ADR-001-architecture-hexagonale-ddd.md)) |
| **E3** Une règle à un seul endroit | Règles dans les agrégats ; l'import CSV réutilise l'enregistrement unitaire ; contrôle d'accès dans l'application ([ADR-004](docs/adr/ADR-004-identite-simulee-et-controle-acces.md)) |
| **E4** Vocabulaire du métier partout | Code, API et documentation en français métier ([glossaire](docs/glossaire.md)) |
| **E5** Contrat OpenAPI fidèle | Généré depuis le code (springdoc), exporté dans `docs/openapi.yaml` |
| **E6** Format d'erreur unique | RFC 9457 `application/problem+json` + `code` stable ([ADR-003](docs/adr/ADR-003-format-erreurs-et-evolution-api.md)) |
| **E7** Évolution sans rupture | Version dans le chemin `/api/v1`, DTO séparés du domaine, évolutions additives ([ADR-003](docs/adr/ADR-003-format-erreurs-et-evolution-api.md)) |
| **E8** Décisions justifiées | 6 ADR dans [`docs/adr`](docs/adr) |

**Valeurs de référence** : aucune n'est écrite en dur dans les règles. Les règles de la coopérative
(50 ha, 50 kg, 5 %, 30 %) sont dans `application.properties` (`sunuagri.cooperative.*`) ; les paramètres
des cultures sont chargés en base au premier démarrage puis modifiables par l'administrateur (US-19).

## 7. Architecture

Monolithe modulaire : un module par **contexte délimité**, chacun en **architecture hexagonale**.

```
sn.sunuagri
├── partage/            noyau partagé : Kilogrammes, MontantXof, Hectares, TypeCulture, Qualite,
│                       Utilisateur, port Transactions, format d'erreur, configuration OpenAPI
├── referentiel/        paramètres des cultures, règles de la coopérative, prix du marché
├── exploitation/       producteurs, parcelles, campagnes, cultures, récoltes
├── stock/              livraisons à l'entrepôt, lots, état du stock
├── commercialisation/  offres, commandes, livraison des commandes
└── paiement/           répartition, paiements dus, notification SMS
    ├── domaine/        agrégats, objets-valeurs, services de domaine, ports (Java pur)
    ├── application/    cas d'usage, contrôle d'accès, ports techniques (Java pur)
    └── infrastructure/ adaptateurs REST, JPA, SMS simulé, assemblage Spring
```

## 8. Codes de réponse

| Code | Signification |
|---|---|
| 200 / 201 | Succès / ressource créée (en-tête `Location`) |
| 400 | Requête mal formée ou champ invalide (`violations` détaille les champs) |
| 401 | En-tête d'identité absent ou invalide |
| 403 | Le rôle ne permet pas l'opération |
| 404 | Ressource introuvable, ou appartenant à un autre utilisateur |
| 409 | Doublon (téléphone, campagne) |
| 422 | Règle métier non respectée (le `code` précise laquelle) |
| 503 | Service externe de prix indisponible et aucun prix connu |
