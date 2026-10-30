# ADR-003 — Contrat d'API : format d'erreur unique et évolution sans rupture

**Statut** : accepté

## Contexte
Le sujet exige un contrat OpenAPI fidèle au comportement réel (E5), un format d'erreur unique et
documenté sans détail interne (E6), et une API qui évolue sans casser les clients (E7).

## Décision
- **Erreurs** : toutes au format **RFC 9457 Problem Details** (`application/problem+json`) avec
  `type`, `title`, `status`, `detail`, `instance`, et les extensions `code` (stable, ex.
  `TELEPHONE_DEJA_UTILISE`), `horodatage` et `violations` (validation). Un seul gestionnaire
  (`GestionnaireErreurs`) traduit toute exception ; une erreur inattendue renvoie un message générique.
- **Statuts** : 400 requête mal formée, 401 identité absente, 403 rôle non autorisé, 404 introuvable
  (ou appartenant à un autre), 409 doublon, 422 règle métier non respectée, 503 service externe
  indisponible sans repli.
- **Ressources** au vocabulaire du métier (`/producteurs`, `/campagnes`, `/offres`…). Les transitions
  d'état sont des **sous-ressources d'action** nommées comme le métier :
  `POST /campagnes/{id}/cloture`, `/cultures/{id}/semis`, `/cultures/{id}/recolte`,
  `/offres/{id}/retrait`, `/commandes/{id}/annulation`, `/commandes/{id}/livraison`.
- **Évolution** : version majeure dans le chemin (`/api/v1`). Seules des évolutions compatibles sont
  faites dans une version (ajout de champs optionnels, de ressources, de codes d'erreur) ; les clients
  doivent ignorer les champs inconnus. Les DTO d'API sont distincts du domaine : le modèle interne peut
  changer sans changer le contrat. Une rupture impose `/api/v2`, avec maintien de `v1` pendant la transition.
- Le contrat est **généré depuis le code** (springdoc) : il ne peut pas diverger du comportement. Les
  réponses d'erreur sont déclarées une fois (`components/responses`) et référencées par chaque opération.
  Il est exporté dans `docs/openapi.yaml`.

## Conséquences
- (+) Un client traite toutes les erreurs de la même façon et peut se fier au `code`.
- (+) Swagger UI documente exactement ce que l'API fait.
- (−) Les annotations OpenAPI alourdissent les contrôleurs.
