package sn.sunuagri.referentiel.infrastructure.prix;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.application.FournisseurPrixMarche;
import sn.sunuagri.referentiel.domaine.PrixReference;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

/**
 * Adaptateur SIMULÉ du service externe de prix du marché.
 * {@code sunuagri.prix-marche.simulation.disponible=false} simule une panne du service.
 */
@Component
@ConfigurationProperties(prefix = "sunuagri.prix-marche.simulation")
public class PrixMarcheSimuleAdaptateur implements FournisseurPrixMarche {

    private final Clock horloge;
    private boolean disponible = true;
    private Map<TypeCulture, Long> prix = new EnumMap<>(TypeCulture.class);

    public PrixMarcheSimuleAdaptateur(Clock horloge) {
        this.horloge = horloge;
    }

    @Override
    public PrixReference prixActuel(TypeCulture culture) {
        if (!disponible) {
            throw new ServiceIndisponible("Service de prix du marché injoignable (simulation).");
        }
        Long valeur = prix.get(culture);
        if (valeur == null) {
            throw new ServiceIndisponible("Aucun prix publié pour " + culture.libelle() + ".");
        }
        return new PrixReference(culture, MontantXof.de(valeur), horloge.instant());
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public void setPrix(Map<TypeCulture, Long> prix) {
        this.prix = prix;
    }
}
