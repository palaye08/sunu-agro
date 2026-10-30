package sn.sunuagri.commercialisation.application;

import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.commercialisation.domaine.offre.OffreRepository;
import sn.sunuagri.commercialisation.domaine.offre.StatutOffre;
import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.stock.application.StockService;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Cas d'usage US-10 : publier, retirer et consulter les offres de vente. */
public class OffreService {

    private final OffreRepository offres;
    private final StockService stock;
    private final ControlePrix controlePrix;
    private final Transactions transactions;
    private final Clock horloge;

    public OffreService(OffreRepository offres, StockService stock, ControlePrix controlePrix,
                        Transactions transactions, Clock horloge) {
        this.offres = offres;
        this.stock = stock;
        this.controlePrix = controlePrix;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public record PublicationOffre(TypeCulture culture, Qualite qualite, long quantiteKg, long prixParKg) {
    }

    /** Contrôle du prix par rapport au marché (US-18). Retourne un avertissement, ou null. */
    @FunctionalInterface
    public interface ControlePrix {
        ControlePrix AUCUN = (culture, prix) -> null;

        String avertissement(TypeCulture culture, long prixParKg);
    }

    public Offre publier(Utilisateur utilisateur, PublicationOffre demande) {
        utilisateur.exigerRole(Role.AGENT);
        String avertissement = demande.culture() == null ? null
                : controlePrix.avertissement(demande.culture(), demande.prixParKg());
        return transactions.executer(() -> {
            Kilogrammes disponible = demande.culture() == null || demande.qualite() == null ? Kilogrammes.ZERO
                    : stock.etat(demande.culture(), demande.qualite()).disponible();
            Offre offre = Offre.publier(demande.culture(), demande.qualite(), demande.quantiteKg(),
                    demande.prixParKg(), disponible, avertissement, horloge.instant());
            offres.enregistrer(offre);
            return offre;
        });
    }

    public Offre retirer(Utilisateur utilisateur, UUID offreId) {
        utilisateur.exigerRole(Role.AGENT);
        return transactions.executer(() -> {
            Offre offre = offres.trouverPourModification(offreId)
                    .orElseThrow(() -> new RessourceIntrouvable("offre", offreId));
            offre.retirer();
            offres.enregistrer(offre);
            return offre;
        });
    }

    /** Un acheteur ne voit que les offres ouvertes ; un agent voit toutes les offres (filtre facultatif). */
    public List<Offre> lister(Utilisateur utilisateur, StatutOffre statut) {
        utilisateur.exigerRole(Role.AGENT, Role.ACHETEUR);
        StatutOffre filtre = utilisateur.estAcheteur() ? StatutOffre.OUVERTE : statut;
        return offres.lister(filtre).stream()
                .sorted(Comparator.comparing(Offre::datePublication).reversed()).toList();
    }

    public Offre consulter(Utilisateur utilisateur, UUID offreId) {
        utilisateur.exigerRole(Role.AGENT, Role.ACHETEUR);
        return offres.trouver(offreId)
                .filter(o -> !utilisateur.estAcheteur() || o.estOuverte())
                .orElseThrow(() -> new RessourceIntrouvable("offre", offreId));
    }
}
