package sn.sunuagri.partage.domaine;

import java.math.BigDecimal;

/** Superficie en hectares. */
public record Hectares(BigDecimal valeur) implements Comparable<Hectares> {

    public static final Hectares ZERO = new Hectares(BigDecimal.ZERO);

    public Hectares {
        if (valeur == null) {
            throw new RegleMetierEnfreinte("SUPERFICIE_OBLIGATOIRE", "La superficie est obligatoire.");
        }
        valeur = valeur.stripTrailingZeros();
    }

    public static Hectares strictementPositive(BigDecimal valeur) {
        RegleMetierEnfreinte.verifier(valeur != null && valeur.signum() > 0, "SUPERFICIE_NON_POSITIVE",
                "La superficie doit être strictement positive.");
        return new Hectares(valeur);
    }

    public static Hectares de(String valeur) {
        return new Hectares(new BigDecimal(valeur));
    }

    public Hectares plus(Hectares autre) {
        return new Hectares(valeur.add(autre.valeur));
    }

    public boolean depasse(Hectares autre) {
        return valeur.compareTo(autre.valeur) > 0;
    }

    @Override
    public int compareTo(Hectares autre) {
        return valeur.compareTo(autre.valeur);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Hectares h && valeur.compareTo(h.valeur) == 0;
    }

    @Override
    public int hashCode() {
        return valeur.hashCode();
    }

    @Override
    public String toString() {
        return valeur.toPlainString() + " ha";
    }
}
