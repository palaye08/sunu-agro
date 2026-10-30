package sn.sunuagri.partage.domaine;

/**
 * La ressource n'existe pas, ou l'utilisateur n'a pas le droit de savoir qu'elle existe
 * (US-17 : demander la ressource d'un autre ne doit pas révéler qu'elle existe).
 */
public class RessourceIntrouvable extends ExceptionMetier {

    public RessourceIntrouvable(String ressource, Object identifiant) {
        super(ressource.toUpperCase().replace(' ', '_') + "_INTROUVABLE",
                "Ressource « " + ressource + " » introuvable : " + identifiant);
    }
}
