package sn.sunuagri.stock.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.sunuagri.exploitation.domaine.culture.CultureRepository;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.stock.application.LivraisonEntrepotService;
import sn.sunuagri.stock.application.StockService;
import sn.sunuagri.stock.domaine.LotRepository;
import sn.sunuagri.stock.domaine.ReservationsPort;

import java.time.Clock;

@Configuration
public class ConfigurationStock {

    @Bean
    StockService stockService(LotRepository lots, ReservationsPort reservations) {
        return new StockService(lots, reservations);
    }

    @Bean
    LivraisonEntrepotService livraisonEntrepotService(CultureRepository cultures, LotRepository lots,
                                                      Transactions transactions, Clock horloge) {
        return new LivraisonEntrepotService(cultures, lots, transactions, horloge);
    }
}
