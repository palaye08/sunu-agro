# ADR-006 — Répartition des paiements au franc près (méthode du plus fort reste)

**Statut** : accepté

## Contexte
US-13 : la commission (5 %) et les paiements dus doivent totaliser exactement le montant de la commande ;
la règle d'arrondi est à définir. Le franc CFA n'a pas de subdivision ; une répartition proportionnelle
produit des fractions de franc.

## Décision
1. Commission = montant × taux, arrondie au franc le plus proche (demi vers le haut).
2. Montant à répartir = montant − commission.
3. Chaque producteur reçoit la partie entière de sa part proportionnelle (calcul en entiers, sans flottant).
4. Les francs restants sont attribués un par un aux plus grands restes ; à égalité, au plus gros apport,
   puis par identifiant (résultat déterministe et reproductible).
5. Un contrôle d'invariant refuse toute répartition dont la somme diffère du montant.

## Conséquences
- (+) Aucun franc ne se perd ni n'apparaît, quel que soit le nombre de producteurs (vérifié par un test
  sur 200 cas aléatoires).
- (+) Écart maximal d'un franc par producteur par rapport à la part exacte, attribué de façon équitable.
- (+) Le taux de commission est un paramètre (`sunuagri.cooperative.taux-commission`).
- (−) Règle à expliquer aux producteurs : elle est décrite ici et dans le document de conception.
