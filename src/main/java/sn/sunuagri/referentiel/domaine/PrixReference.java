package sn.sunuagri.referentiel.domaine;

import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Prix de référence du marché pour une culture, à une date donnée (US-18). */
public record PrixReference(TypeCulture culture, MontantXof prixParKg, Instant date) {

    public PrixReference {
        Objects.requireNonNull(culture);
        Objects.requireNonNull(prixParKg);
        Objects.requireNonNull(date);
    }

    /** Écart relatif |prix − référence| / référence. */
    public BigDecimal ecartRelatif(long prixPropose) {
        BigDecimal reference = BigDecimal.valueOf(prixParKg.valeur());
        return BigDecimal.valueOf(prixPropose).subtract(reference).abs().divide(reference, 4, RoundingMode.HALF_UP);
    }

    /** Avertissement si l'écart dépasse le seuil ; l'offre n'est jamais refusée pour autant. */
    public Optional<String> avertissementSiEcart(long prixPropose, BigDecimal seuil) {
        if (prixParKg.valeur() == 0) {
            return Optional.empty();
        }
        BigDecimal ecart = ecartRelatif(prixPropose);
        if (ecart.compareTo(seuil) <= 0) {
            return Optional.empty();
        }
        String pourcentage = ecart.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).toPlainString();
        return Optional.of("Attention : le prix de " + prixPropose + " XOF/kg s'écarte de " + pourcentage
                + " % du prix de référence du marché (" + prixParKg.valeur() + " XOF/kg au " + date + "), au-delà de "
                + seuil.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + " %.");
    }
}
