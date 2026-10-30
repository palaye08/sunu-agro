package sn.sunuagri.partage.domaine;

/** Une demande bien formée qui enfreint une règle du métier. */
public class RegleMetierEnfreinte extends ExceptionMetier {

    public RegleMetierEnfreinte(String code, String message) {
        super(code, message);
    }

    /** Vérifie une règle et lève l'exception si elle n'est pas respectée. */
    public static void verifier(boolean condition, String code, String message) {
        if (!condition) {
            throw new RegleMetierEnfreinte(code, message);
        }
    }
}
