package sn.sunuagri.commercialisation.application;

import sn.sunuagri.commercialisation.domaine.commande.Commande;
import sn.sunuagri.commercialisation.domaine.commande.CommandeRepository;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;
import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.commercialisation.domaine.offre.OffreRepository;
import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.PageDemandee;
import sn.sunuagri.partage.domaine.PageResultat;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;

import java.time.Clock;
import java.util.UUID;

/** Cas d'usage US-11 (passer), US-12 (annuler) et US-16 (historique). */
public class CommandeService {

    private final CommandeRepository commandes;
    private final OffreRepository offres;
    private final ReglesCooperative regles;
    private final Transactions transactions;
    private final Clock horloge;

    public CommandeService(CommandeRepository commandes, OffreRepository offres, ReglesCooperative regles,
                           Transactions transactions, Clock horloge) {
        this.commandes = commandes;
        this.offres = offres;
        this.regles = regles;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public Commande passer(Utilisateur utilisateur, UUID offreId, long quantiteKg) {
        utilisateur.exigerRole(Role.ACHETEUR);
        return transactions.executer(() -> {
            Offre offre = offres.trouverPourModification(offreId)
                    .filter(Offre::estOuverte)
                    .orElseThrow(() -> new RessourceIntrouvable("offre", offreId));
            Commande commande = Commande.passer(offre, utilisateur.identifiant(), quantiteKg,
                    regles.quantiteMinimaleCommande(), horloge.instant());
            offres.enregistrer(offre);
            commandes.enregistrer(commande);
            return commande;
        });
    }

    public Commande annuler(Utilisateur utilisateur, UUID commandeId) {
        utilisateur.exigerRole(Role.ACHETEUR);
        return transactions.executer(() -> {
            Commande commande = commandeVisible(utilisateur, commandeId);
            Offre offre = offres.trouverPourModification(commande.offreId()).orElseThrow();
            commande.annuler(offre);
            offres.enregistrer(offre);
            commandes.enregistrer(commande);
            return commande;
        });
    }

    public Commande consulter(Utilisateur utilisateur, UUID commandeId) {
        utilisateur.exigerRole(Role.ACHETEUR, Role.AGENT);
        return commandeVisible(utilisateur, commandeId);
    }

    /** US-16 : un acheteur ne voit que ses commandes ; de la plus récente à la plus ancienne ; paginé. */
    public PageResultat<Commande> historique(Utilisateur utilisateur, StatutCommande statut, PageDemandee page) {
        utilisateur.exigerRole(Role.ACHETEUR, Role.AGENT);
        String acheteur = utilisateur.estAcheteur() ? utilisateur.identifiant() : null;
        return commandes.rechercher(acheteur, statut, page);
    }

    /** La commande d'un autre acheteur est « introuvable » : on ne révèle pas qu'elle existe (US-17). */
    private Commande commandeVisible(Utilisateur utilisateur, UUID commandeId) {
        return commandes.trouver(commandeId)
                .filter(c -> utilisateur.estAgent() || c.appartientA(utilisateur.identifiant()))
                .orElseThrow(() -> new RessourceIntrouvable("commande", commandeId));
    }
}
