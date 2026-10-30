# ADR-001 — Architecture hexagonale par contexte délimité

**Statut** : accepté

## Contexte
Les règles métier de la coopérative (superficies, rendements, humidité, réservations, répartition des
paiements) sont nombreuses et sources de litiges. Le sujet exige qu'elles soient testées rapidement sans
serveur ni base (E1), qu'on puisse changer de base, de fournisseur SMS ou de source de prix sans les
toucher (E2), et qu'une règle n'existe qu'à un seul endroit (E3).

## Décision
- Découpage en **contextes délimités** (Référentiel, Exploitation, Stock, Commercialisation, Paiement)
  dans un **monolithe modulaire** : un package par contexte.
- Dans chaque contexte, trois couches :
  - `domaine` : agrégats, objets-valeurs, services de domaine et **ports de sortie** (dépôts) — Java pur ;
  - `application` : cas d'usage (un service par groupe de stories), contrôle d'accès, transactions via le
    port `Transactions` — Java pur, sans Spring ;
  - `infrastructure` : adaptateurs d'entrée (REST) et de sortie (JPA, SMS simulé, prix simulé) et
    assemblage Spring (`@Configuration`).
- Les dépendances vont de l'infrastructure vers l'application puis vers le domaine. Ces règles sont
  **vérifiées automatiquement** par `ArchitectureHexagonaleTest` (ArchUnit).

## Conséquences
- (+) 260+ tests métier s'exécutent en moins de 2 secondes, sans Spring ni base (adaptateurs en mémoire).
- (+) Remplacer PostgreSQL, le SMS ou la source de prix = écrire un nouvel adaptateur du port.
- (+) Les règles sont dans les agrégats : l'API, l'import CSV et les tests passent par le même code.
- (−) Plus de classes (entités JPA distinctes des agrégats, traduction dans les adaptateurs).
- (−) Un seul déploiement : les contextes ne peuvent pas évoluer à des rythmes de déploiement différents
  (acceptable pour une coopérative ; un découpage en services resterait possible grâce aux ports).
