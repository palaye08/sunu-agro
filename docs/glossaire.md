# Glossaire SunuAgri

Ce vocabulaire est celui de la coopérative. On le retrouve tel quel dans le code (classes, méthodes),
dans le contrat d'API (ressources, champs) et dans les messages d'erreur (exigence E4).

| Terme métier | Définition | Dans le code | Dans l'API |
|---|---|---|---|
| **Producteur** | Agriculteur membre de la coopérative. Identifié par son numéro de téléphone, unique dans la coopérative. | `Producteur` (agrégat) | `/producteurs` |
| **Téléphone** | Numéro sénégalais normalisé au format `+221XXXXXXXXX` (9 chiffres commençant par 7 ou 3). | `Telephone` (objet-valeur) | `telephone` |
| **Parcelle** | Terrain d'un producteur : nom, superficie en hectares, commune. Appartient à un seul producteur. | `Parcelle` (entité de l'agrégat Producteur) | `/producteurs/{id}/parcelles` |
| **Superficie** | Surface en hectares (ha), strictement positive. | `Hectares` | `superficieHa` |
| **Campagne** | Saison agricole d'une année : hivernage ou contre-saison. Ouverte puis clôturée. | `Campagne`, `Saison`, `StatutCampagne` | `/campagnes` |
| **Hivernage** | Saison des pluies. | `Saison.HIVERNAGE` | `HIVERNAGE` |
| **Contre-saison** | Saison sèche (cultures irriguées). | `Saison.CONTRE_SAISON` | `CONTRE_SAISON` |
| **Clôture** | Fin d'une campagne : elle n'accepte plus de nouvelle culture ni de récolte. | `Campagne.cloturer()` | `POST /campagnes/{id}/cloture` |
| **Culture** | Plante cultivée (arachide, mil, riz, maïs) sur une parcelle pendant une campagne. | `Culture`, `TypeCulture` | `/cultures` |
| **Superficie emblavée** | Surface d'une parcelle ensemencée pour une culture. | `superficieEmblavee` | `superficieEmblaveeHa` |
| **Planifiée / semée / récoltée** | Statuts successifs d'une culture, sans retour en arrière. | `StatutCulture` | `PLANIFIEE`, `SEMEE`, `RECOLTEE` |
| **Semis** | Mise en terre des semences, à une date donnée. | `Culture.semer()` | `POST /cultures/{id}/semis` |
| **Récolte** | Quantité produite par une culture, en kilogrammes, déclarée une seule fois. | `Culture.recolter()` | `POST /cultures/{id}/recolte` |
| **Rendement** | Quantité récoltée divisée par la superficie emblavée (kg/ha). | `rendementKgParHa()` | `rendementKgParHa` |
| **Rendement de référence** | Rendement normal d'une culture, paramétrable. Au-delà de 2 fois ce rendement, une récolte est **suspecte**. | `ParametresCulture` | `/parametres-cultures` |
| **Humidité maximale** | Taux d'humidité au-delà duquel une livraison à l'entrepôt est refusée. | `humiditeMaxPourcent` | `humiditeMaxPourcent` |
| **Cycle minimal** | Nombre minimal de jours entre le semis et la récolte. | `cycleMinimalJours` | `cycleMinimalJours` |
| **Paramètres agronomiques** | Rendement, humidité maximale et cycle minimal **figés** sur une culture au moment de sa déclaration. | `ParametresAgronomiques` | — |
| **Livraison à l'entrepôt** | Apport d'une partie d'une récolte à la coopérative ; crée un lot. | `LivraisonEntrepotService` | `POST /cultures/{id}/livraisons` |
| **Lot** | Quantité reçue d'un producteur, datée, avec sa qualité et son taux d'humidité. | `Lot` (agrégat) | `/lots/{id}` |
| **Qualité** | Classement d'un lot à la réception : A, B ou C. | `Qualite` | `qualite` |
| **Stock** | Pour une culture et une qualité : total (dans les lots), réservé (engagé dans des offres), disponible (total − réservé). | `EtatStock` | `/stocks` |
| **Offre** | Quantité d'une culture et d'une qualité mise en vente à un prix au kilogramme. Ouverte, épuisée ou retirée. | `Offre`, `StatutOffre` | `/offres` |
| **Réservation** | Quantité qu'une offre retient sur le stock. | `Offre.quantiteReservee()` | `reserveKg` |
| **Retrait** | Fin d'une offre ; la quantité non vendue retourne au stock disponible. | `Offre.retirer()` | `POST /offres/{id}/retrait` |
| **Acheteur** | Grossiste ou transformateur qui commande à la coopérative. | `Role.ACHETEUR` | `X-Role: ACHETEUR` |
| **Commande** | Demande d'un acheteur sur une offre : en attente, livrée ou annulée. Minimum 50 kg. | `Commande`, `StatutCommande` | `/commandes` |
| **Montant** | Quantité × prix au kg, en francs CFA (XOF), sans subdivision. | `MontantXof` | `montant`, `devise: XOF` |
| **Livraison d'une commande** | Remise de la marchandise à l'acheteur : consomme les lots du plus ancien au plus récent et déclenche la répartition. | `LivraisonCommandeService` | `POST /commandes/{id}/livraison` |
| **Commission** | Part retenue par la coopérative (5 % par défaut) avant répartition. | `ReglesCooperative.tauxCommission` | `commissionCooperative` |
| **Apport** | Quantité qu'un producteur a fournie dans les lots consommés par une commande. | `Apport` | `lotsConsommes` |
| **Répartition** | Partage du montant d'une commande entre la commission et les producteurs, au prorata de leurs apports. | `RepartitionPaiement` | — |
| **Paiement dû** | Part du montant d'une commande qui revient à un producteur, au statut « à payer ». | `PaiementDu` | `/paiements/{id}` |
| **Notification SMS** | Message en français prévenant le producteur d'un paiement dû ; envoyé une seule fois. | `StatutNotification` | `notificationSms` |
| **Prix de référence du marché** | Prix au kg d'une culture fourni par un service externe ; le dernier prix connu sert de repli. | `PrixReference` | `/prix-marche/{culture}` |
| **Agent de coopérative** | Employé qui enregistre, suit, reçoit et vend. | `Role.AGENT` | `X-Role: AGENT` |
| **Administrateur** | Responsable des paramètres de la plateforme. | `Role.ADMINISTRATEUR` | `X-Role: ADMINISTRATEUR` |
