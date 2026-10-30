package sn.sunuagri.commercialisation.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.sunuagri.commercialisation.application.CommandeService;
import sn.sunuagri.commercialisation.application.LivraisonCommandeService;
import sn.sunuagri.commercialisation.application.OffreService;
import sn.sunuagri.commercialisation.domaine.commande.CommandeRepository;
import sn.sunuagri.commercialisation.domaine.offre.OffreRepository;
import sn.sunuagri.paiement.application.NotificationPaiementService;
import sn.sunuagri.paiement.application.PaiementService;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;
import sn.sunuagri.stock.application.StockService;
import sn.sunuagri.stock.domaine.LotRepository;

import java.time.Clock;

@Configuration
public class ConfigurationCommercialisation {

    @Bean
    OffreService offreService(OffreRepository offres, StockService stock, Transactions transactions, Clock horloge) {
        return new OffreService(offres, stock, OffreService.ControlePrix.AUCUN, transactions, horloge);
    }

    @Bean
    CommandeService commandeService(CommandeRepository commandes, OffreRepository offres, ReglesCooperative regles,
                                    Transactions transactions, Clock horloge) {
        return new CommandeService(commandes, offres, regles, transactions, horloge);
    }

    @Bean
    LivraisonCommandeService livraisonCommandeService(CommandeRepository commandes, OffreRepository offres,
                                                      LotRepository lots, PaiementService paiements,
                                                      NotificationPaiementService notifications,
                                                      Transactions transactions, Clock horloge) {
        return new LivraisonCommandeService(commandes, offres, lots, paiements, notifications, transactions, horloge);
    }
}
