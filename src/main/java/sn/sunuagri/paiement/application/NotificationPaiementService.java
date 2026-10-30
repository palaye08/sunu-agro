package sn.sunuagri.paiement.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.sunuagri.paiement.domaine.PaiementDu;
import sn.sunuagri.paiement.domaine.PaiementDuRepository;
import sn.sunuagri.partage.application.Transactions;

import java.time.Clock;
import java.util.UUID;

/**
 * Cas d'usage US-14 : prévenir un producteur par SMS qu'un paiement lui est dû.
 * Appelé APRÈS la validation de la livraison : un échec d'envoi ne remet jamais la livraison en cause,
 * il est tracé sur le paiement (statut ECHEC, motif, date).
 */
public class NotificationPaiementService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationPaiementService.class);

    private final PaiementDuRepository paiements;
    private final AnnuaireProducteurs annuaire;
    private final EnvoiSms sms;
    private final Transactions transactions;
    private final Clock horloge;

    public NotificationPaiementService(PaiementDuRepository paiements, AnnuaireProducteurs annuaire, EnvoiSms sms,
                                       Transactions transactions, Clock horloge) {
        this.paiements = paiements;
        this.annuaire = annuaire;
        this.sms = sms;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public PaiementDu notifier(UUID paiementId) {
        return transactions.executer(() -> {
            PaiementDu paiement = paiements.trouver(paiementId).orElseThrow();
            if (!paiement.doitEtreNotifie()) {
                return paiement;
            }
            try {
                String numero = annuaire.telephone(paiement.producteurId())
                        .orElseThrow(() -> new EnvoiSms.EchecEnvoiSms("Numéro du producteur introuvable."));
                sms.envoyer(numero, paiement.messageSms());
                paiement.notificationEnvoyee(horloge.instant());
            } catch (RuntimeException e) {
                LOG.warn("Échec d'envoi du SMS pour le paiement {} : {}", paiementId, e.getMessage());
                paiement.notificationEchouee(e.getMessage() == null ? "Erreur inconnue" : e.getMessage(), horloge.instant());
            }
            paiements.enregistrer(paiement);
            return paiement;
        });
    }
}
