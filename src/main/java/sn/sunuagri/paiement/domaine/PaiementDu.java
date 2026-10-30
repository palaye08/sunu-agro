package sn.sunuagri.paiement.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat PaiementDu : part du montant d'une commande qui revient à un producteur (US-13),
 * avec le suivi de la notification SMS (US-14).
 */
public class PaiementDu {

    private final UUID id;
    private final UUID commandeId;
    private final UUID producteurId;
    private final TypeCulture culture;
    private final Kilogrammes quantite;
    private final MontantXof montant;
    private final StatutPaiement statut;
    private final Instant dateCreation;
    private StatutNotification notification;
    private Instant dateNotification;
    private String motifEchecNotification;

    private PaiementDu(UUID id, UUID commandeId, UUID producteurId, TypeCulture culture, Kilogrammes quantite,
                       MontantXof montant, StatutPaiement statut, Instant dateCreation, StatutNotification notification,
                       Instant dateNotification, String motifEchecNotification) {
        this.id = Objects.requireNonNull(id);
        this.commandeId = commandeId;
        this.producteurId = producteurId;
        this.culture = culture;
        this.quantite = quantite;
        this.montant = montant;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.notification = notification;
        this.dateNotification = dateNotification;
        this.motifEchecNotification = motifEchecNotification;
    }

    /** Chaque paiement dû est enregistré avec le statut « à payer ». */
    public static PaiementDu enregistrer(UUID commandeId, TypeCulture culture, Repartition.Part part, Instant maintenant) {
        return new PaiementDu(UUID.randomUUID(), commandeId, part.producteurId(), culture, part.quantite(),
                part.montant(), StatutPaiement.A_PAYER, maintenant, StatutNotification.EN_ATTENTE, null, null);
    }

    public static PaiementDu reconstituer(UUID id, UUID commandeId, UUID producteurId, TypeCulture culture,
                                          Kilogrammes quantite, MontantXof montant, StatutPaiement statut,
                                          Instant dateCreation, StatutNotification notification,
                                          Instant dateNotification, String motifEchecNotification) {
        return new PaiementDu(id, commandeId, producteurId, culture, quantite, montant, statut, dateCreation,
                notification, dateNotification, motifEchecNotification);
    }

    /** Un SMS n'est envoyé qu'une seule fois par paiement. */
    public boolean doitEtreNotifie() {
        return notification != StatutNotification.ENVOYEE;
    }

    /** Message SMS en français, indiquant le montant. */
    public String messageSms() {
        return "SunuAgri : un paiement de " + montant.enFrancais() + " vous est dû pour la vente de "
                + quantite.valeur() + " kg " + culture.precedeDeDe() + ". Statut : à payer.";
    }

    public void notificationEnvoyee(Instant quand) {
        notification = StatutNotification.ENVOYEE;
        dateNotification = quand;
        motifEchecNotification = null;
    }

    public void notificationEchouee(String motif, Instant quand) {
        notification = StatutNotification.ECHEC;
        dateNotification = quand;
        motifEchecNotification = motif;
    }

    public UUID id() {
        return id;
    }

    public UUID commandeId() {
        return commandeId;
    }

    public UUID producteurId() {
        return producteurId;
    }

    public TypeCulture culture() {
        return culture;
    }

    public Kilogrammes quantite() {
        return quantite;
    }

    public MontantXof montant() {
        return montant;
    }

    public StatutPaiement statut() {
        return statut;
    }

    public Instant dateCreation() {
        return dateCreation;
    }

    public StatutNotification notification() {
        return notification;
    }

    public Instant dateNotification() {
        return dateNotification;
    }

    public String motifEchecNotification() {
        return motifEchecNotification;
    }
}
