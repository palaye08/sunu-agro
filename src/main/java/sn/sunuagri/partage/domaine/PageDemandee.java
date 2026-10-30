package sn.sunuagri.partage.domaine;


/** Pagination demandée par un client (page commençant à 0). */
public record PageDemandee(int page, int taille) {

    public static final int TAILLE_MAX = 100;

    public PageDemandee {
        RegleMetierEnfreinte.verifier(page >= 0, "PAGINATION_INVALIDE", "Le numéro de page doit être positif ou nul.");
        RegleMetierEnfreinte.verifier(taille >= 1 && taille <= TAILLE_MAX, "PAGINATION_INVALIDE",
                "La taille de page doit être comprise entre 1 et " + TAILLE_MAX + ".");
    }
}
