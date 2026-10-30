package sn.sunuagri.partage.domaine;

/**
 * Racine des erreurs métier. Le {@code code} est stable et fait partie du contrat d'API :
 * les clients peuvent s'y fier, contrairement au message qui peut évoluer.
 */
public abstract class ExceptionMetier extends RuntimeException {

    private final String code;

    protected ExceptionMetier(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
