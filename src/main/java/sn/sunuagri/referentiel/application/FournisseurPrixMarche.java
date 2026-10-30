package sn.sunuagri.referentiel.application;

import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.PrixReference;

/** Port de sortie : service externe de prix du marché (remplaçable sans toucher aux règles, E2). */
public interface FournisseurPrixMarche {

    /** @throws ServiceIndisponible si le service ne répond pas */
    PrixReference prixActuel(TypeCulture culture);

    class ServiceIndisponible extends RuntimeException {
        public ServiceIndisponible(String message) {
            super(message);
        }
    }
}
