package sn.sunuagri.commercialisation.domaine.offre;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat Offre : quantité d'une culture et d'une qualité mise en vente à un prix au kilogramme (US-10).
 *
 * <ul>
 *   <li>quantité commandée = commandes en attente + commandes livrées ;</li>
 *   <li>quantité restante = quantité de l'offre − quantité commandée (ce qu'un acheteur peut encore commander) ;</li>
 *   <li>quantité réservée dans le stock = ce que l'offre retient encore sur le stock (voir {@link #quantiteReservee()}).</li>
 * </ul>
 */
public class Offre {

    private final UUID id;
    private final TypeCulture culture;
    private final Qualite qualite;
    private final Kilogrammes quantite;
    private final MontantXof prixParKg;
    private final Instant datePublication;
    private final String avertissementPrix;
    private StatutOffre statut;
    private Kilogrammes quantiteCommandee;
    private Kilogrammes quantiteLivree;

    private Offre(UUID id, TypeCulture culture, Qualite qualite, Kilogrammes quantite, MontantXof prixParKg,
                  Instant datePublication, String avertissementPrix, StatutOffre statut,
                  Kilogrammes quantiteCommandee, Kilogrammes quantiteLivree) {
        this.id = Objects.requireNonNull(id);
        this.culture = culture;
        this.qualite = qualite;
        this.quantite = quantite;
        this.prixParKg = prixParKg;
        this.datePublication = datePublication;
        this.avertissementPrix = avertissementPrix;
        this.statut = statut;
        this.quantiteCommandee = quantiteCommandee;
        this.quantiteLivree = quantiteLivree;
    }

    /**
     * Publie une offre : quantité ≤ stock disponible de cette culture et qualité, prix strictement positif.
     * L'offre publiée réserve aussitôt sa quantité dans le stock.
     */
    public static Offre publier(TypeCulture culture, Qualite qualite, long quantiteKg, long prixParKg,
                                Kilogrammes stockDisponible, String avertissementPrix, Instant maintenant) {
        RegleMetierEnfreinte.verifier(culture != null && qualite != null, "CULTURE_ET_QUALITE_OBLIGATOIRES",
                "La culture et la qualité de l'offre sont obligatoires.");
        Kilogrammes quantite = Kilogrammes.strictementPositive(quantiteKg);
        RegleMetierEnfreinte.verifier(prixParKg > 0, "PRIX_NON_POSITIF",
                "Le prix au kilogramme doit être strictement positif.");
        RegleMetierEnfreinte.verifier(!quantite.depasse(stockDisponible), "STOCK_DISPONIBLE_INSUFFISANT",
                "La quantité de l'offre (" + quantite + ") dépasse le stock disponible en " + culture.libelle()
                        + " de qualité " + qualite + " (" + stockDisponible + ").");
        return new Offre(UUID.randomUUID(), culture, qualite, quantite, MontantXof.de(prixParKg), maintenant,
                avertissementPrix, StatutOffre.OUVERTE, Kilogrammes.ZERO, Kilogrammes.ZERO);
    }

    public static Offre reconstituer(UUID id, TypeCulture culture, Qualite qualite, Kilogrammes quantite,
                                     MontantXof prixParKg, Instant datePublication, String avertissementPrix,
                                     StatutOffre statut, Kilogrammes quantiteCommandee, Kilogrammes quantiteLivree) {
        return new Offre(id, culture, qualite, quantite, prixParKg, datePublication, avertissementPrix, statut,
                quantiteCommandee, quantiteLivree);
    }

    /**
     * US-11 : une commande prend une quantité sur l'offre ouverte. Minimum de commande respecté,
     * quantité ≤ restante ; l'offre devient épuisée quand plus rien ne reste.
     */
    public void prendreCommande(Kilogrammes demandee, Kilogrammes minimumCommande) {
        RegleMetierEnfreinte.verifier(statut == StatutOffre.OUVERTE, "OFFRE_NON_OUVERTE",
                "L'offre n'est pas ouverte (statut : " + statut + ").");
        RegleMetierEnfreinte.verifier(!minimumCommande.depasse(demandee), "COMMANDE_MINIMALE_NON_ATTEINTE",
                "La quantité minimale d'une commande est de " + minimumCommande + " (demandé : " + demandee + ").");
        RegleMetierEnfreinte.verifier(!demandee.depasse(quantiteRestante()), "QUANTITE_OFFRE_INSUFFISANTE",
                "La quantité commandée (" + demandee + ") dépasse la quantité restante de l'offre (" + quantiteRestante() + ").");
        quantiteCommandee = quantiteCommandee.plus(demandee);
        if (quantiteRestante().estNul()) {
            statut = StatutOffre.EPUISEE;
        }
    }

    /** US-12 : la quantité d'une commande annulée redevient disponible dans l'offre. */
    public void restituerCommandeAnnulee(Kilogrammes quantiteAnnulee) {
        quantiteCommandee = quantiteCommandee.moins(quantiteAnnulee);
        if (statut == StatutOffre.EPUISEE) {
            statut = StatutOffre.OUVERTE;
        }
    }

    /** US-13 : la marchandise d'une commande livrée quitte l'entrepôt. */
    public void enregistrerLivraison(Kilogrammes quantiteLivreeCommande) {
        quantiteLivree = quantiteLivree.plus(quantiteLivreeCommande);
    }

    /** US-10 : retirer l'offre libère la quantité non vendue ; les commandes en attente restent honorées. */
    public void retirer() {
        RegleMetierEnfreinte.verifier(statut != StatutOffre.RETIREE, "OFFRE_DEJA_RETIREE", "L'offre est déjà retirée.");
        statut = StatutOffre.RETIREE;
    }

    public Kilogrammes quantiteRestante() {
        return quantite.moins(quantiteCommandee);
    }

    /**
     * Quantité que l'offre retient dans le stock :
     * toute la quantité non encore livrée si l'offre est ouverte ou épuisée ;
     * seulement les commandes en attente si elle est retirée.
     */
    public Kilogrammes quantiteReservee() {
        return statut == StatutOffre.RETIREE ? quantiteCommandee.moins(quantiteLivree) : quantite.moins(quantiteLivree);
    }

    public boolean estOuverte() {
        return statut == StatutOffre.OUVERTE;
    }

    public UUID id() {
        return id;
    }

    public TypeCulture culture() {
        return culture;
    }

    public Qualite qualite() {
        return qualite;
    }

    public Kilogrammes quantite() {
        return quantite;
    }

    public MontantXof prixParKg() {
        return prixParKg;
    }

    public Instant datePublication() {
        return datePublication;
    }

    public String avertissementPrix() {
        return avertissementPrix;
    }

    public StatutOffre statut() {
        return statut;
    }

    public Kilogrammes quantiteCommandee() {
        return quantiteCommandee;
    }

    public Kilogrammes quantiteLivree() {
        return quantiteLivree;
    }
}
