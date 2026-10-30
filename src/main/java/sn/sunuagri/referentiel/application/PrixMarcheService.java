package sn.sunuagri.referentiel.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.application.ServiceExterneIndisponible;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.DernierPrixConnuRepository;
import sn.sunuagri.referentiel.domaine.PrixReference;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;

import java.util.Optional;

/**
 * Cas d'usage US-18 : consulter le prix de référence du marché.
 * Si le service externe est indisponible, le dernier prix connu est rendu avec sa date
 * et l'application continue de fonctionner.
 */
public class PrixMarcheService {

    private static final Logger LOG = LoggerFactory.getLogger(PrixMarcheService.class);

    private final FournisseurPrixMarche fournisseur;
    private final DernierPrixConnuRepository derniersPrix;
    private final ReglesCooperative regles;

    public PrixMarcheService(FournisseurPrixMarche fournisseur, DernierPrixConnuRepository derniersPrix,
                             ReglesCooperative regles) {
        this.fournisseur = fournisseur;
        this.derniersPrix = derniersPrix;
        this.regles = regles;
    }

    public enum Source { SERVICE_EXTERNE, DERNIER_PRIX_CONNU }

    public record PrixMarche(PrixReference prix, Source source) {
    }

    public static class PrixMarcheIndisponible extends ServiceExterneIndisponible {
        public PrixMarcheIndisponible(TypeCulture culture) {
            super("PRIX_MARCHE_INDISPONIBLE", "Le service de prix du marché est indisponible et aucun prix n'est connu pour "
                    + culture.libelle() + ".");
        }
    }

    public PrixMarche consulter(Utilisateur utilisateur, TypeCulture culture) {
        utilisateur.exigerRole(Role.AGENT);
        return prixDuMarche(culture).orElseThrow(() -> new PrixMarcheIndisponible(culture));
    }

    /** Avertissement d'écart de prix pour une offre ; null si pas d'écart ou si aucun prix n'est connu. */
    public String avertissementPourOffre(TypeCulture culture, long prixParKg) {
        return prixDuMarche(culture)
                .flatMap(p -> p.prix().avertissementSiEcart(prixParKg, regles.ecartPrixAvertissement()))
                .orElse(null);
    }

    private Optional<PrixMarche> prixDuMarche(TypeCulture culture) {
        try {
            PrixReference prix = fournisseur.prixActuel(culture);
            derniersPrix.enregistrer(prix);
            return Optional.of(new PrixMarche(prix, Source.SERVICE_EXTERNE));
        } catch (RuntimeException e) {
            LOG.warn("Service de prix indisponible pour {} : {}", culture, e.getMessage());
            return derniersPrix.trouver(culture).map(p -> new PrixMarche(p, Source.DERNIER_PRIX_CONNU));
        }
    }
}
