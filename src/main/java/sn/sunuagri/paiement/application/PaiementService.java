package sn.sunuagri.paiement.application;

import sn.sunuagri.paiement.domaine.Apport;
import sn.sunuagri.paiement.domaine.PaiementDu;
import sn.sunuagri.paiement.domaine.PaiementDuRepository;
import sn.sunuagri.paiement.domaine.Repartition;
import sn.sunuagri.paiement.domaine.RepartitionPaiement;
import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Cas d'usage US-13 (enregistrer les paiements dus) et consultation des paiements (US-17). */
public class PaiementService {

    private final PaiementDuRepository paiements;
    private final ReglesCooperative regles;
    private final Clock horloge;

    public PaiementService(PaiementDuRepository paiements, ReglesCooperative regles, Clock horloge) {
        this.paiements = paiements;
        this.regles = regles;
        this.horloge = horloge;
    }

    public record PaiementsDus(Repartition repartition, List<PaiementDu> paiements) {
    }

    /** À appeler dans la transaction de livraison de la commande. */
    public PaiementsDus enregistrerPaiementsDus(UUID commandeId, TypeCulture culture, MontantXof montant,
                                                List<Apport> apports) {
        Repartition repartition = RepartitionPaiement.repartir(montant, regles.tauxCommission(), apports);
        List<PaiementDu> crees = repartition.parts().stream()
                .map(part -> PaiementDu.enregistrer(commandeId, culture, part, horloge.instant()))
                .toList();
        crees.forEach(paiements::enregistrer);
        return new PaiementsDus(repartition, crees);
    }

    public List<PaiementDu> duProducteur(Utilisateur utilisateur, UUID producteurId) {
        utilisateur.exigerRole(Role.AGENT, Role.PRODUCTEUR);
        if (!utilisateur.peutVoirDonneesDuProducteur(producteurId)) {
            throw new RessourceIntrouvable("producteur", producteurId);
        }
        return paiements.duProducteur(producteurId).stream()
                .sorted(Comparator.comparing(PaiementDu::dateCreation).reversed()).toList();
    }

    public PaiementDu consulter(Utilisateur utilisateur, UUID paiementId) {
        utilisateur.exigerRole(Role.AGENT, Role.PRODUCTEUR);
        return paiements.trouver(paiementId)
                .filter(p -> utilisateur.peutVoirDonneesDuProducteur(p.producteurId()))
                .orElseThrow(() -> new RessourceIntrouvable("paiement", paiementId));
    }
}
