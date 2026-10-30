package sn.sunuagri.commercialisation.domaine.commande;

import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Agrégat Commande : demande d'un acheteur sur une offre (US-11, US-12, US-13). */
public class Commande {

    private final UUID id;
    private final UUID offreId;
    private final String acheteurId;
    private final TypeCulture culture;
    private final Qualite qualite;
    private final Kilogrammes quantite;
    private final MontantXof prixParKg;
    private final MontantXof montant;
    private final Instant dateCreation;
    private StatutCommande statut;
    private Instant dateLivraison;

    private Commande(UUID id, UUID offreId, String acheteurId, TypeCulture culture, Qualite qualite, Kilogrammes quantite,
                     MontantXof prixParKg, MontantXof montant, Instant dateCreation, StatutCommande statut,
                     Instant dateLivraison) {
        this.id = Objects.requireNonNull(id);
        this.offreId = offreId;
        this.acheteurId = acheteurId;
        this.culture = culture;
        this.qualite = qualite;
        this.quantite = quantite;
        this.prixParKg = prixParKg;
        this.montant = montant;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.dateLivraison = dateLivraison;
    }

    /**
     * US-11 : la commande prend sa quantité sur l'offre ; montant = quantité × prix au kg ;
     * statut initial « en attente ».
     */
    public static Commande passer(Offre offre, String acheteurId, long quantiteKg, Kilogrammes minimumCommande,
                                  Instant maintenant) {
        Kilogrammes quantite = Kilogrammes.strictementPositive(quantiteKg);
        offre.prendreCommande(quantite, minimumCommande);
        MontantXof montant = MontantXof.de(Math.multiplyExact(quantite.valeur(), offre.prixParKg().valeur()));
        return new Commande(UUID.randomUUID(), offre.id(), acheteurId, offre.culture(), offre.qualite(), quantite,
                offre.prixParKg(), montant, maintenant, StatutCommande.EN_ATTENTE, null);
    }

    public static Commande reconstituer(UUID id, UUID offreId, String acheteurId, TypeCulture culture, Qualite qualite,
                                        Kilogrammes quantite, MontantXof prixParKg, MontantXof montant,
                                        Instant dateCreation, StatutCommande statut, Instant dateLivraison) {
        return new Commande(id, offreId, acheteurId, culture, qualite, quantite, prixParKg, montant, dateCreation,
                statut, dateLivraison);
    }

    /** US-12 : annulable tant qu'elle n'est pas livrée ; la quantité revient à l'offre. */
    public void annuler(Offre offre) {
        RegleMetierEnfreinte.verifier(statut != StatutCommande.LIVREE, "COMMANDE_DEJA_LIVREE",
                "Une commande livrée ne peut pas être annulée.");
        RegleMetierEnfreinte.verifier(statut != StatutCommande.ANNULEE, "COMMANDE_DEJA_ANNULEE",
                "La commande est déjà annulée.");
        statut = StatutCommande.ANNULEE;
        offre.restituerCommandeAnnulee(quantite);
    }

    /** US-13 : une commande déjà livrée ou annulée ne peut pas être livrée. */
    public void livrer(Offre offre, Instant maintenant) {
        RegleMetierEnfreinte.verifier(statut == StatutCommande.EN_ATTENTE, "COMMANDE_NON_LIVRABLE",
                "Seule une commande en attente peut être livrée (statut actuel : " + statut + ").");
        statut = StatutCommande.LIVREE;
        dateLivraison = maintenant;
        offre.enregistrerLivraison(quantite);
    }

    public boolean appartientA(String autreAcheteurId) {
        return Objects.equals(acheteurId, autreAcheteurId);
    }

    public UUID id() {
        return id;
    }

    public UUID offreId() {
        return offreId;
    }

    public String acheteurId() {
        return acheteurId;
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

    public MontantXof montant() {
        return montant;
    }

    public Instant dateCreation() {
        return dateCreation;
    }

    public StatutCommande statut() {
        return statut;
    }

    public Instant dateLivraison() {
        return dateLivraison;
    }
}
