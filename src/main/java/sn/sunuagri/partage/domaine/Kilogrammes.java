package sn.sunuagri.partage.domaine;

/**
 * Quantité en kilogrammes. Hypothèse : la coopérative pèse au kilogramme près,
 * les quantités sont donc des entiers (pas de fraction de kilogramme).
 */
public record Kilogrammes(long valeur) implements Comparable<Kilogrammes> {

    public static final Kilogrammes ZERO = new Kilogrammes(0);

    public Kilogrammes {
        if (valeur < 0) {
            throw new RegleMetierEnfreinte("QUANTITE_NEGATIVE", "Une quantité ne peut pas être négative.");
        }
    }

    public static Kilogrammes de(long valeur) {
        return new Kilogrammes(valeur);
    }

    /** Fabrique une quantité strictement positive. */
    public static Kilogrammes strictementPositive(long valeur) {
        RegleMetierEnfreinte.verifier(valeur > 0, "QUANTITE_NON_POSITIVE",
                "La quantité doit être strictement positive (reçu : " + valeur + " kg).");
        return new Kilogrammes(valeur);
    }

    public Kilogrammes plus(Kilogrammes autre) {
        return new Kilogrammes(valeur + autre.valeur);
    }

    public Kilogrammes moins(Kilogrammes autre) {
        return new Kilogrammes(valeur - autre.valeur);
    }

    public Kilogrammes min(Kilogrammes autre) {
        return compareTo(autre) <= 0 ? this : autre;
    }

    public boolean depasse(Kilogrammes autre) {
        return valeur > autre.valeur;
    }

    public boolean estNul() {
        return valeur == 0;
    }

    @Override
    public int compareTo(Kilogrammes autre) {
        return Long.compare(valeur, autre.valeur);
    }

    @Override
    public String toString() {
        return valeur + " kg";
    }
}
