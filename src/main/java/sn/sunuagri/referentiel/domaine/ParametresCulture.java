package sn.sunuagri.referentiel.domaine;

import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Paramètres agronomiques de référence d'une culture (agrégat du contexte Référentiel).
 * Les valeurs sont strictement positives (US-19).
 */
public record ParametresCulture(
        TypeCulture culture,
        BigDecimal rendementReferenceKgParHa,
        BigDecimal humiditeMaxPourcent,
        int cycleMinimalJours) {

    public ParametresCulture {
        Objects.requireNonNull(culture, "culture");
        RegleMetierEnfreinte.verifier(rendementReferenceKgParHa != null && rendementReferenceKgParHa.signum() > 0,
                "PARAMETRE_CULTURE_INVALIDE", "Le rendement de référence doit être strictement positif.");
        RegleMetierEnfreinte.verifier(humiditeMaxPourcent != null && humiditeMaxPourcent.signum() > 0
                        && humiditeMaxPourcent.compareTo(BigDecimal.valueOf(100)) <= 0,
                "PARAMETRE_CULTURE_INVALIDE", "L'humidité maximale doit être strictement positive et au plus 100 %.");
        RegleMetierEnfreinte.verifier(cycleMinimalJours > 0,
                "PARAMETRE_CULTURE_INVALIDE", "Le cycle minimal doit être strictement positif.");
    }

    /** Une modification produit de nouveaux paramètres ; les cultures déjà déclarées gardent les leurs. */
    public ParametresCulture modifier(BigDecimal rendement, BigDecimal humiditeMax, int cycleMinimal) {
        return new ParametresCulture(culture, rendement, humiditeMax, cycleMinimal);
    }
}
