# ADR-002 — Relations entre contextes et cohérence transactionnelle

**Statut** : accepté

## Contexte
Plusieurs stories traversent des contextes : la livraison à l'entrepôt (Exploitation → Stock), le stock
disponible (Stock ← offres de la Commercialisation), la livraison d'une commande (Commercialisation →
Stock → Paiement) et le SMS (Paiement ← téléphone de l'Exploitation). Il faut éviter les dépendances
circulaires et garantir qu'une opération refusée ne modifie rien (US-08, US-13).

## Décision
- Quand un contexte a besoin d'une information d'un autre **sans devoir le connaître**, il définit un
  **port** que l'autre implémente (inversion de dépendance) :
  - `ReservationsPort` (Stock) implémenté par l'adaptateur des offres ;
  - `AnnuaireProducteurs` (Paiement) implémenté par l'Exploitation.
- Les cas d'usage qui orchestrent plusieurs agrégats le font **dans une seule transaction locale**
  (base unique) : `LivraisonEntrepotService` (culture + lot), `LivraisonCommandeService`
  (commande + offre + lots + paiements dus).
- Le **SMS est envoyé après la validation** de la transaction de livraison, chacun dans sa propre
  transaction : un échec SMS ne peut pas annuler une livraison (US-14).

## Conséquences
- (+) Cohérence forte et simple : une livraison est entièrement faite ou pas du tout.
- (+) Pas de cycle de dépendances entre contextes.
- (−) Plusieurs agrégats modifiés dans une transaction : acceptable dans un monolithe à base unique.
  Si les contextes devenaient des services séparés, on passerait à des événements de domaine
  (`CommandeLivree` → répartition) avec cohérence à terme.
