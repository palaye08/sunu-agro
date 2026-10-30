package sn.sunuagri.paiement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.sunuagri.paiement.application.AnnuaireProducteurs;
import sn.sunuagri.paiement.application.EnvoiSms;
import sn.sunuagri.paiement.application.NotificationPaiementService;
import sn.sunuagri.paiement.application.PaiementService;
import sn.sunuagri.paiement.domaine.PaiementDuRepository;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;

import java.time.Clock;

@Configuration
public class ConfigurationPaiement {

    @Bean
    PaiementService paiementService(PaiementDuRepository paiements, ReglesCooperative regles, Clock horloge) {
        return new PaiementService(paiements, regles, horloge);
    }

    @Bean
    NotificationPaiementService notificationPaiementService(PaiementDuRepository paiements, AnnuaireProducteurs annuaire,
                                                            EnvoiSms sms, Transactions transactions, Clock horloge) {
        return new NotificationPaiementService(paiements, annuaire, sms, transactions, horloge);
    }
}
