# Document de conception — SunuAgri

Ce document présente les contextes délimités, les agrégats, les règles métier de chaque story
réalisée et les hypothèses retenues lorsque le sujet était ambigu.

## 1. Contextes délimités (bounded contexts)

L'application est un **monolithe modulaire** : un seul déploiement, un module Java par contexte
(`sn.sunuagri.<contexte>`), chacun organisé en hexagone (`domaine`, `application`, `infrastructure`).

```
                     ┌──────────────────────┐
                     │      Référentiel     │  paramètres des cultures, règles de la
                     │ (US-18, US-19)       │  coopérative, prix du marché
                     └─────────┬────────────┘
                               │ paramètres en vigueur, règles
          ┌────────────────────┼─────────────────────────────┐
          ▼                    ▼                             ▼
┌───────────────────┐  ┌───────────────────┐  ┌─────────────────────────┐
│    Exploitation   │  │       Stock       │  │    Commercialisation    │
│ producteurs,      │─▶│ lots, état du     │◀─│ offres, commandes,      │
│ parcelles,        │  │ stock, conso FIFO │  │ livraison des commandes │
│ campagnes,        │  │ (US-08, US-09)    │  │ (US-10 à US-13, US-16)  │
│ cultures, récoltes│  └───────────────────┘  └───────────┬─────────────┘
│ (US-01 à US-07,   │                                     │ apports
│  US-15)           │◀──── téléphone du producteur ───┐   ▼
└───────────────────┘                          ┌──────┴──────────────────┐
                                               │        Paiement         │
                                               │ répartition, paiements  │
                                               │ dus, SMS (US-13, US-14) │
                                               └─────────────────────────┘
```

| Contexte | Responsabilité | Langage propre |
|---|---|---|
| **Référentiel** | Valeurs de référence modifiables (cultures, coopérative) et prix du marché | paramètres, rendement de référence, seuil, prix de référence |
| **Exploitation** | Ce qui se passe « au champ » | producteur, parcelle, campagne, culture, semis, récolte |
| **Stock** | Ce qui se passe « à l'entrepôt » | livraison à l'entrepôt, lot, qualité, total / réservé / disponible |
| **Commercialisation** | La vente | offre, commande, acheteur, montant |
| **Paiement** | La rémunération des producteurs | commission, apport, répartition, paiement dû, notification |
| **Partagé** (noyau) | Types communs et techniques transverses | kilogrammes, montant XOF, hectares, type de culture, qualité, utilisateur |

**Relations entre contextes** (voir ADR-002) :
- *Stock → Commercialisation* : le Stock définit le port `ReservationsPort` (quantité réservée),
  implémenté par la Commercialisation (inversion de dépendance : le Stock ne connaît pas les offres).
- *Paiement → Exploitation* : le Paiement définit le port `AnnuaireProducteurs` (téléphone),
  implémenté par l'Exploitation.
- *Commercialisation → Stock / Paiement* : la livraison d'une commande orchestre la consommation des
  lots (service de domaine du Stock) et l'enregistrement des paiements dus (service applicatif du Paiement).
- *Stock → Exploitation* : la livraison à l'entrepôt demande à la culture de valider la livraison.

## 2. Agrégats

| Agrégat (racine) | Contexte | Contenu | Invariants protégés |
|---|---|---|---|
| `Producteur` | Exploitation | producteur + ses `Parcelle` | noms de parcelles distincts ; superficie totale ≤ 50 ha |
| `Campagne` | Exploitation | saison, année, dates, statut | fin > début ; clôturée ⇒ ni culture ni récolte |
| `Culture` | Exploitation | parcelle, campagne, type, superficie emblavée, paramètres figés, semis, récolte, cumul livré | statut sans retour arrière ; cycle minimal ; récolte unique et non suspecte ; cumul livré ≤ récolte ; humidité ≤ seuil |
| `Lot` | Stock | producteur, culture, qualité, humidité, quantités initiale et restante, date | restant ≥ 0 |
| `Offre` | Commercialisation | culture, qualité, quantité, prix, quantités commandée et livrée, statut | commandé ≤ quantité ; épuisée quand rien ne reste ; seule une offre ouverte prend des commandes |
| `Commande` | Commercialisation | offre, acheteur, quantité, prix figé, montant, statut | montant = quantité × prix ; transitions en attente → livrée / annulée |
| `PaiementDu` | Paiement | commande, producteur, quantité, montant, statut, notification | SMS envoyé au plus une fois |
| `ParametresCulture` | Référentiel | rendement, humidité max, cycle minimal | valeurs strictement positives |

**Pourquoi les parcelles sont dans l'agrégat Producteur** : les deux invariants de US-02 portent sur
l'ensemble des parcelles d'un producteur. Les placer dans l'agrégat garantit qu'ils sont vérifiés en un seul endroit.

**Pourquoi Culture est un agrégat séparé** : une culture a son propre cycle de vie (semis, récolte,
livraisons) et est modifiée bien plus souvent que la parcelle. La règle « somme des superficies emblavées
≤ superficie de la parcelle » est vérifiée par `Culture.declarer(...)`, qui reçoit la parcelle et les
cultures existantes de la parcelle pour la campagne.

**Services de domaine** (règles qui portent sur plusieurs agrégats, sans état) :
- `ConsommationLots` (Stock) : consommation FIFO des lots ;
- `RepartitionPaiement` (Paiement) : commission et répartition au franc près.

## 3. Règles métier par story

Toutes les règles sont dans le **domaine** ; les codes d'erreur (champ `code` des réponses d'erreur)
sont stables et font partie du contrat.

### US-01 · Enregistrer un producteur — *Indispensable* ✅
| Règle | Où | Code d'erreur |
|---|---|---|
| Nom, prénom, village obligatoires | `Producteur.enregistrer` | `CHAMP_OBLIGATOIRE` (422) |
| Téléphone valide, normalisé `+221…` | `Telephone.de` | `TELEPHONE_INVALIDE` (422) |
| Téléphone unique dans la coopérative | `ProducteurService` + contrainte d'unicité en base | `TELEPHONE_DEJA_UTILISE` (409) |
| Un identifiant est attribué | `Producteur.enregistrer` (UUID) | — |

### US-02 · Déclarer une parcelle — *Indispensable* ✅
| Règle | Où | Code |
|---|---|---|
| Superficie strictement positive | `Hectares.strictementPositive` | `SUPERFICIE_NON_POSITIVE` |
| Total des superficies ≤ 50 ha (paramétrable) | `Producteur.declarerParcelle` | `SUPERFICIE_MAX_DEPASSEE` |
| Noms distincts pour un même producteur (sans tenir compte de la casse ni des espaces) | `Producteur.declarerParcelle` | `NOM_PARCELLE_DEJA_UTILISE` |
| Une parcelle appartient à un seul producteur | structure de l'agrégat | — |

### US-03 · Consulter les parcelles d'un producteur — *Importante* ✅
Liste et détail (nom, commune, superficie). Producteur inexistant → 404 `PRODUCTEUR_INTROUVABLE`.

### US-04 · Ouvrir et clôturer une campagne — *Indispensable* ✅
| Règle | Où | Code |
|---|---|---|
| Saison HIVERNAGE ou CONTRE_SAISON | type `Saison` (valeur inconnue ⇒ 400) | `REQUETE_INVALIDE` |
| Date de fin postérieure à la date de début | `Campagne.ouvrir` | `DATES_CAMPAGNE_INVALIDES` |
| Une campagne par saison et par année | `CampagneService` + contrainte d'unicité | `CAMPAGNE_DEJA_EXISTANTE` (409) |
| Clôturée ⇒ plus de culture ni de récolte | `Campagne.verifierOuverte` | `CAMPAGNE_CLOTUREE` |

### US-05 · Déclarer une culture — *Indispensable* ✅
| Règle | Où | Code |
|---|---|---|
| Campagne ouverte | `Culture.declarer` | `CAMPAGNE_CLOTUREE` |
| Culture de la liste de référence | type `TypeCulture` | `CULTURE_INCONNUE` / 400 |
| Superficie emblavée > 0 | `Hectares.strictementPositive` | `SUPERFICIE_NON_POSITIVE` |
| Σ superficies emblavées (parcelle, campagne) ≤ superficie de la parcelle | `Culture.declarer` | `SUPERFICIE_PARCELLE_DEPASSEE` |
| Statut initial « planifiée » | `Culture.declarer` | — |

### US-06 · Suivre l'avancement d'une culture — *Importante* ✅
| Règle | Où | Code |
|---|---|---|
| Planifiée → semée → récoltée, sans retour | `Culture.semer`, `Culture.recolter` | `TRANSITION_STATUT_INVALIDE`, `CULTURE_NON_SEMEE`, `CULTURE_DEJA_RECOLTEE` |
| Date de récolte ≥ date de semis + cycle minimal | `Culture.recolter` | `CYCLE_MINIMAL_NON_RESPECTE` |

### US-07 · Déclarer une récolte — *Indispensable* ✅
| Règle | Où | Code |
|---|---|---|
| Culture semée | `Culture.recolter` | `CULTURE_NON_SEMEE` |
| Quantité > 0 | `Kilogrammes.strictementPositive` | `QUANTITE_NON_POSITIVE` |
| Rendement ≤ 2 × rendement de référence | `Culture.recolter` | `RECOLTE_SUSPECTE` |
| Récoltée une seule fois | `Culture.recolter` | `CULTURE_DEJA_RECOLTEE` |
| Campagne ouverte (US-04) | `Campagne.verifierOuverte` | `CAMPAGNE_CLOTUREE` |

### US-08 · Livrer une récolte à l'entrepôt — *Indispensable* ✅
| Règle | Où | Code |
|---|---|---|
| Culture récoltée | `Culture.livrerALEntrepot` | `CULTURE_NON_RECOLTEE` |
| Cumul livré ≤ quantité récoltée | `Culture.livrerALEntrepot` | `QUANTITE_LIVREE_SUPERIEURE_A_RECOLTE` |
| Humidité ≤ seuil de la culture | `Culture.livrerALEntrepot` | `HUMIDITE_TROP_ELEVEE` |
| Livraison acceptée ⇒ lot identifiable, rattaché au producteur, daté | `Lot.receptionner` | — |
| Livraison refusée ⇒ rien n'est modifié | une seule transaction, contrôles avant toute écriture | — |

### US-09 · Consulter le stock — *Importante* ✅
- Par culture et par qualité : **total** (somme des restes des lots), **réservé** (somme des
  `Offre.quantiteReservee()`), **disponible = total − réservé**.
- Les chiffres sont **calculés à la demande** à partir des lots et des offres : ils restent cohérents
  après chaque livraison, vente, annulation ou retrait, sans compteur à maintenir (ADR-005).

### US-10 · Publier une offre — *Indispensable* ✅
| Règle | Où | Code |
|---|---|---|
| Quantité ≤ stock disponible (culture, qualité) | `Offre.publier` | `STOCK_DISPONIBLE_INSUFFISANT` |
| Prix au kg > 0 | `Offre.publier` | `PRIX_NON_POSITIF` |
| L'offre publiée réserve sa quantité | `Offre.quantiteReservee()` | — |
| Ouverte, épuisée ou retirée ; retirer libère le non-vendu | `Offre.retirer`, `quantiteReservee()` | `OFFRE_DEJA_RETIREE` |

### US-11 · Passer une commande — *Indispensable* ✅
| Règle | Où | Code |
|---|---|---|
| Offre ouverte | `Offre.prendreCommande` (un acheteur ne voit pas les offres non ouvertes ⇒ 404) | `OFFRE_INTROUVABLE` |
| Quantité ≥ 50 kg (paramétrable) | `Offre.prendreCommande` | `COMMANDE_MINIMALE_NON_ATTEINTE` |
| Quantité ≤ restant de l'offre | `Offre.prendreCommande` | `QUANTITE_OFFRE_INSUFFISANTE` |
| Montant = quantité × prix au kg | `Commande.passer` | — |
| Statut « en attente » ; offre épuisée quand rien ne reste | `Commande.passer`, `Offre.prendreCommande` | — |

### US-12 · Annuler une commande — *Importante* ✅
| Règle | Où | Code |
|---|---|---|
| Possible tant que non livrée | `Commande.annuler` | `COMMANDE_DEJA_LIVREE`, `COMMANDE_DEJA_ANNULEE` |
| La quantité redevient disponible dans l'offre (une offre épuisée redevient ouverte) | `Offre.restituerCommandeAnnulee` | — |
| Seul l'acheteur propriétaire peut annuler | `CommandeService` | 404 |

### US-13 · Livrer une commande et répartir le paiement — *Indispensable* ✅
| Règle | Où |
|---|---|
| Seule une commande en attente est livrable | `Commande.livrer` → `COMMANDE_NON_LIVRABLE` |
| Lots de la culture et de la qualité consommés du plus ancien au plus récent | `ConsommationLots.consommer` (tri par date de réception, puis identifiant) |
| Commission de 5 % retenue avant répartition | `RepartitionPaiement.repartir` |
| Répartition au prorata des quantités apportées dans les lots consommés | `RepartitionPaiement.repartir` (apports cumulés par producteur) |
| Commission + paiements = montant, exactement | règle d'arrondi ci-dessous + contrôle d'invariant + test sur 200 cas aléatoires |
| Chaque paiement dû est « à payer » | `PaiementDu.enregistrer` |

**Règle d'arrondi (documentée, ADR-006)** — le franc CFA n'a pas de subdivision :
1. `commission = arrondi(montant × 5 %)` au franc le plus proche, demi vers le haut ;
2. `à répartir = montant − commission` ;
3. chaque producteur reçoit `⌊à répartir × apport / apport total⌋` ;
4. les francs restants (moins que le nombre de producteurs) vont un par un aux producteurs ayant les
   **plus grands restes** de division ; à reste égal, au plus gros apport, puis par identifiant.

*Exemple* : 600 kg à 350 F = 210 000 F ; commission 10 500 F ; Awa (400 kg) 133 000 F ;
Modou (200 kg) 66 500 F ; 10 500 + 133 000 + 66 500 = 210 000 F.

### US-14 · Être prévenu par SMS d'un paiement — *Importante* ✅
| Règle | Où |
|---|---|
| SMS en français indiquant le montant (« SunuAgri : un paiement de 133 000 FCFA vous est dû pour la vente de 400 kg d'arachide. Statut : à payer. ») | `PaiementDu.messageSms` |
| Un seul SMS par paiement | `PaiementDu.doitEtreNotifie` (statut ENVOYEE ⇒ plus d'envoi) |
| Échec d'envoi ⇒ livraison toujours valide, échec tracé | envoi **après** la validation de la livraison ; statut ECHEC + motif + date sur le paiement ; journal WARN |

L'envoi est **simulé** (`SmsSimuleAdaptateur`, journal `[SMS SIMULÉ]`). La propriété
`sunuagri.sms.simulation.numeros-en-echec` liste les numéros qui simulent une panne.

### US-15 · Importer des producteurs en lot — *Importante* ✅
- Fichier CSV `nom;prénom;téléphone;village` (UTF-8, en-tête facultatif).
- **Exactement les mêmes règles** qu'US-01 : chaque ligne passe par le même code (`ProducteurService.enregistrerProducteur`), E3.
- Lignes valides enregistrées (une transaction par ligne), lignes invalides rapportées avec numéro et motif,
  compte rendu final (traitées, acceptées, refusées). Un doublon à l'intérieur du fichier est détecté.

### US-16 · Historique des commandes — *Importante* ✅
- Pagination **en base** (`page`, `taille` ≤ 100), tri du plus récent au plus ancien, filtre `statut`.
- Index `(acheteurId, statut, dateCreation)` pour garder des réponses rapides avec beaucoup de commandes.
- Un acheteur ne voit que ses commandes (filtre imposé par l'application, pas par le client).

### US-17 · Accéder uniquement à ce qui me concerne — *Indispensable* ✅
| Rôle | Peut voir / faire |
|---|---|
| PRODUCTEUR | ses parcelles, cultures (et récoltes), lots, paiements |
| ACHETEUR | offres ouvertes ; passer, consulter, annuler **ses** commandes |
| AGENT | toutes les données de la coopérative ; toutes les opérations de gestion |
| ADMINISTRATEUR | paramètres des cultures |

- Rôle non autorisé pour une opération ⇒ **403** `ACCES_REFUSE`.
- Ressource d'un autre ⇒ **404** identique à une ressource inexistante (on ne révèle pas son existence).
- Contrôles faits dans la couche **application**, donc identiques quel que soit le canal (API, import CSV…).

### US-18 · Prix de référence du marché — *Bonus* ✅
- Port `FournisseurPrixMarche` ; adaptateur simulé (`sunuagri.prix-marche.simulation.disponible=false` simule une panne).
- Chaque prix obtenu est mémorisé ; si le service est indisponible, le **dernier prix connu** est rendu
  avec sa date (`source = DERNIER_PRIX_CONNU`). Sans aucun prix connu : 503, et la publication d'offre continue de fonctionner.
- Offre dont le prix s'écarte de plus de 30 % : publiée avec `avertissementPrix`, jamais refusée.

### US-19 · Paramètres des cultures — *Bonus* ✅
- Consultation et modification par l'ADMINISTRATEUR ; valeurs strictement positives.
- Les paramètres sont **copiés dans la culture à sa déclaration** (`ParametresAgronomiques`) : une
  modification s'applique aux déclarations futures, jamais aux anciennes.

## 4. Hypothèses retenues (règles ambiguës)

| # | Sujet | Hypothèse |
|---|---|---|
| H1 | Unités | Les quantités sont des **kilogrammes entiers** ; les montants des **francs CFA entiers**. Les superficies et l'humidité sont décimales. |
| H2 | Téléphone | Numéro sénégalais : 9 chiffres commençant par 7 ou 3, préfixe `+221`/`00221` facultatif ; stocké normalisé pour garantir l'unicité. |
| H3 | Récolte (US-06 / US-07) | « Déclarer la récolte » et « passer au statut récoltée » sont un **seul fait métier** : une seule opération enregistre la date et la quantité. |
| H4 | Rendement suspect | Suspect si strictement supérieur à 2 × la référence ; exactement 2 × est accepté. |
| H5 | Cycle minimal | La récolte est acceptée à partir de `date de semis + cycle minimal` (inclus). |
| H6 | Livraison à l'entrepôt | Possible seulement après la récolte. Elle reste possible après la clôture de la campagne (le sujet n'interdit que culture et récolte). |
| H7 | Semis | Le semis n'est pas contraint par les dates de la campagne (le sujet ne le demande pas). |
| H8 | Réservation | Une offre ouverte ou épuisée réserve sa quantité non encore livrée ; une offre retirée ne réserve plus que les commandes en attente (elles restent honorées). |
| H9 | Annulation sur offre retirée | La quantité annulée retourne au stock disponible (l'offre reste retirée). |
| H10 | Commande minimale | Le minimum de 50 kg s'applique toujours, même si le reste d'une offre est inférieur. |
| H11 | Producteurs multiples | Plusieurs lots d'un même producteur consommés par une commande sont cumulés en un seul paiement dû. |
| H12 | Paiement | Le versement réel est hors périmètre : un paiement dû reste « à payer ». |
| H13 | Coopérative unique | La plateforme sert une seule coopérative : un agent voit toutes les données. |
| H14 | Identité | Simulée par les en-têtes `X-Role` et `X-Utilisateur-Id` (hors périmètre, ADR-004). Les acheteurs ne sont pas enregistrés : leur identifiant est celui fourni. |
| H15 | Concurrence | L'offre est verrouillée pendant une commande, une annulation ou une livraison pour ne jamais survendre ; une contrainte d'unicité en base protège téléphone et campagne. Limite connue : deux publications d'offres strictement simultanées sur le même stock ne sont pas sérialisées (usage par un seul agent à la fois supposé). |
