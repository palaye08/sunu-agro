package sn.sunuagri.partage.infrastructure.web;

/** Références vers les réponses d'erreur décrites une seule fois dans le contrat OpenAPI. */
public final class ReponsesErreur {

    public static final String REQUETE_INVALIDE = "#/components/responses/RequeteInvalide";
    public static final String ACCES_REFUSE = "#/components/responses/AccesRefuse";
    public static final String INTROUVABLE = "#/components/responses/Introuvable";
    public static final String CONFLIT = "#/components/responses/Conflit";
    public static final String REGLE_METIER = "#/components/responses/RegleMetier";

    private ReponsesErreur() {
    }
}
