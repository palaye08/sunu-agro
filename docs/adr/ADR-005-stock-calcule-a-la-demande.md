# ADR-005 — Stock calculé à la demande plutôt que compteurs maintenus

**Statut** : accepté

## Contexte
US-09 exige que total, réservé et disponible restent cohérents après chaque livraison, vente,
annulation ou retrait. Maintenir des compteurs mis à jour par chaque opération multiplie les endroits
où une erreur peut créer un écart (exactement le problème des tableurs de la coopérative).

## Décision
- **Total** = somme des quantités restantes des lots (source de vérité physique).
- **Réservé** = somme de `Offre.quantiteReservee()`, règle écrite une seule fois dans l'agrégat Offre
  (copiée dans une colonne à chaque enregistrement pour pouvoir sommer en base).
- **Disponible** = total − réservé, calculé par `EtatStock`.

## Conséquences
- (+) Impossible d'avoir un stock incohérent : il est déduit des faits (lots, offres).
- (+) Une seule règle de réservation, testée unitairement.
- (−) Une requête d'agrégation par consultation : négligeable au volume d'une coopérative, et
  indexée par (culture, qualité).
