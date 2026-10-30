package sn.sunuagri.commercialisation.application;

import sn.sunuagri.commercialisation.domaine.commande.Commande;
import sn.sunuagri.commercialisation.domaine.commande.CommandeRepository;
import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.commercialisation.domaine.offre.OffreRepository;
import sn.sunuagri.paiement.application.NotificationPaiementService;
import sn.sunuagri.paiement.application.PaiementService;
import sn.sunuagri.paiement.domaine.Apport;
import sn.sunuagri.paiement.domaine.PaiementDu;
import sn.sunuagri.paiement.domaine.Repartition;
import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;
import sn.sunuagri.stock.domaine.ConsommationLots;
import sn.sunuagri.stock.domaine.Lot;
import sn.sunuagri.stock.domaine.LotRepository;
import sn.sunuagri.stock.domaine.Prelevement;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage US-13 : confirmer la livraison d'une commande et répartir le paiement.
 *
 * <ol>
 *   <li>Dans UNE transaction : la commande passe à « livrée », les lots de la culture et de la qualité sont
 *       consommés du plus ancien au plus récent, le montant est réparti et les paiements dus enregistrés.</li>
 *   <li>Après validation : un SMS est envoyé pour chaque paiement (US-14). Un échec d'envoi est tracé
 *       mais ne remet pas en cause la livraison.</li>
 * </ol>
 */
public class LivraisonCommandeService {

    private final CommandeRepository commandes;
    private final OffreRepository offres;
    private final LotRepository lots;
    private final PaiementService paiements;
    private final NotificationPaiementService notifications;
    private final Transactions transactions;
    private final Clock horloge;

    public LivraisonCommandeService(CommandeRepository commandes, OffreRepository offres, LotRepository lots,
                                    PaiementService paiements, NotificationPaiementService notifications,
                                    Transactions transactions, Clock horloge) {
        this.commandes = commandes;
        this.offres = offres;
        this.lots = lots;
        this.paiements = paiements;
        this.notifications = notifications;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public record LivraisonCommande(Commande commande, List<Prelevement> lotsConsommes, Repartition repartition,
                                    List<PaiementDu> paiements) {
    }

    public LivraisonCommande livrer(Utilisateur utilisateur, UUID commandeId) {
        utilisateur.exigerRole(Role.AGENT);
        LivraisonCommande livraison = transactions.executer(() -> {
            Commande commande = commandes.trouver(commandeId)
                    .orElseThrow(() -> new RessourceIntrouvable("commande", commandeId));
            Offre offre = offres.trouverPourModification(commande.offreId()).orElseThrow();
            commande.livrer(offre, horloge.instant());

            List<Lot> candidats = lots.nonEpuises(commande.culture(), commande.qualite());
            List<Prelevement> prelevements = ConsommationLots.consommer(candidats, commande.quantite());
            candidats.stream()
                    .filter(l -> prelevements.stream().anyMatch(p -> p.lotId().equals(l.id())))
                    .forEach(lots::enregistrer);

            List<Apport> apports = prelevements.stream().map(p -> new Apport(p.producteurId(), p.quantite())).toList();
            PaiementService.PaiementsDus dus = paiements.enregistrerPaiementsDus(commande.id(), commande.culture(),
                    commande.montant(), apports);

            offres.enregistrer(offre);
            commandes.enregistrer(commande);
            return new LivraisonCommande(commande, prelevements, dus.repartition(), dus.paiements());
        });

        List<PaiementDu> notifies = livraison.paiements().stream().map(p -> notifications.notifier(p.id())).toList();
        return new LivraisonCommande(livraison.commande(), livraison.lotsConsommes(), livraison.repartition(), notifies);
    }
}
