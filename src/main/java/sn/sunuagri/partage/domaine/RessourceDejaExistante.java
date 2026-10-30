package sn.sunuagri.partage.domaine;

/** Une ressource identique existe déjà (unicité métier). */
public class RessourceDejaExistante extends ExceptionMetier {

    public RessourceDejaExistante(String code, String message) {
        super(code, message);
    }
}
