package sn.sunuagri.partage.application;

import sn.sunuagri.partage.domaine.ExceptionMetier;

/** L'identité de l'utilisateur est absente ou invalide. */
public class NonAuthentifie extends ExceptionMetier {

    public NonAuthentifie(String message) {
        super("NON_AUTHENTIFIE", message);
    }
}
