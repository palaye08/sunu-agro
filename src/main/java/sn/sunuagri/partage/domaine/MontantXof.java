package sn.sunuagri.partage.domaine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Montant en francs CFA (XOF). Le franc CFA n'a pas de subdivision : montant entier. */
public record MontantXof(long valeur) implements Comparable<MontantXof> {

    public static final MontantXof ZERO = new MontantXof(0);

    public MontantXof {
        if (valeur < 0) {
            throw new RegleMetierEnfreinte("MONTANT_NEGATIF", "Un montant ne peut pas être négatif.");
        }
    }

    public static MontantXof de(long valeur) {
        return new MontantXof(valeur);
    }

    public MontantXof plus(MontantXof autre) {
        return new MontantXof(valeur + autre.valeur);
    }

    public MontantXof moins(MontantXof autre) {
        return new MontantXof(valeur - autre.valeur);
    }

    /** Applique un taux, arrondi au franc le plus proche (demi vers le haut). */
    public MontantXof multiplierParTaux(BigDecimal taux) {
        return new MontantXof(BigDecimal.valueOf(valeur).multiply(taux)
                .setScale(0, RoundingMode.HALF_UP).longValueExact());
    }

    /** Montant formaté en français, ex. « 125 000 FCFA ». */
    public String enFrancais() {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.FRANCE);
        return format.format(valeur).replace(' ', ' ').replace(' ', ' ') + " FCFA";
    }

    @Override
    public int compareTo(MontantXof autre) {
        return Long.compare(valeur, autre.valeur);
    }

    @Override
    public String toString() {
        return valeur + " XOF";
    }
}
