package sn.sunuagri.commercialisation.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.sunuagri.commercialisation.application.CommandeService;
import sn.sunuagri.commercialisation.application.OffreService;
import sn.sunuagri.commercialisation.domaine.commande.CommandeRepository;
import sn.sunuagri.commercialisation.domaine.offre.OffreRepository;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;
import sn.sunuagri.stock.application.StockService;

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
}
