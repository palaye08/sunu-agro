package sn.sunuagri.partage.application;

import sn.sunuagri.partage.domaine.ExceptionMetier;

/** Le rôle de l'utilisateur ne permet pas cette opération. */
public class AccesRefuse extends ExceptionMetier {

    public AccesRefuse(String message) {
        super("ACCES_REFUSE", message);
    }
}
