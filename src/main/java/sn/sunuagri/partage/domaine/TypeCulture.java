package sn.sunuagri.partage.domaine;

/**
 * Liste de référence des cultures suivies par la coopérative.
 * Partagée par tous les contextes (noyau partagé). Les paramètres agronomiques
 * (rendement, humidité, cycle) ne sont PAS ici : ils sont paramétrables (US-19).
 */
public enum TypeCulture {
    ARACHIDE("arachide"),
    MIL("mil"),
    RIZ("riz"),
    MAIS("maïs");

    private final String libelle;

    TypeCulture(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /** Complément de nom en français, avec élision : « d'arachide », « de mil ». */
    public String precedeDeDe() {
        return "aeiouyéèê".indexOf(libelle.charAt(0)) >= 0 ? "d'" + libelle : "de " + libelle;
    }
}
