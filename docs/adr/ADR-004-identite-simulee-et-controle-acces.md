# ADR-004 — Identité simulée et contrôle d'accès dans la couche application

**Statut** : accepté

## Contexte
L'inscription et la connexion sont hors périmètre ; l'identité peut être simulée. US-17 exige pourtant
que chacun ne voie que ses données, et que demander la ressource d'un autre ne révèle pas son existence.

## Décision
- L'identité est lue dans deux en-têtes : `X-Role` (AGENT, PRODUCTEUR, ACHETEUR, ADMINISTRATEUR) et
  `X-Utilisateur-Id` (id du producteur ou de l'acheteur), par un adaptateur d'entrée
  (`ResolveurUtilisateur`) qui produit un `Utilisateur`.
- Les **règles d'accès sont dans les cas d'usage** (couche application), pas dans les contrôleurs :
  elles s'appliquent quel que soit le canal (REST, import CSV, futur batch).
- Rôle non autorisé ⇒ 403 ; ressource d'un autre ⇒ **404 identique** à une ressource inexistante.

## Conséquences
- (+) Remplacer la simulation par une vraie authentification (OIDC/JWT) ne touche que l'adaptateur
  d'entrée.
- (+) Les règles d'accès sont testées sans serveur.
- (−) En l'état, n'importe quel client peut se déclarer AGENT : acceptable uniquement parce que
  l'authentification est explicitement hors périmètre.
