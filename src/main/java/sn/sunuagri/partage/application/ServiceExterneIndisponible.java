package sn.sunuagri.partage.application;

import sn.sunuagri.partage.domaine.ExceptionMetier;

/** Un service externe ne répond pas et aucune valeur de repli n'est disponible. */
public class ServiceExterneIndisponible extends ExceptionMetier {

    public ServiceExterneIndisponible(String code, String message) {
        super(code, message);
    }
}
