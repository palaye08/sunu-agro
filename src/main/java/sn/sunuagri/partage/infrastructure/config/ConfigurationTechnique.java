package sn.sunuagri.partage.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import sn.sunuagri.partage.application.Transactions;

import java.time.Clock;

@Configuration
public class ConfigurationTechnique {

    @Bean
    Clock horloge() {
        return Clock.systemUTC();
    }

    /** Adaptateur du port {@link Transactions} vers le gestionnaire de transactions Spring. */
    @Bean
    Transactions transactions(PlatformTransactionManager gestionnaire) {
        TransactionTemplate modele = new TransactionTemplate(gestionnaire);
        return new Transactions() {
            @Override
            public <T> T executer(java.util.function.Supplier<T> traitement) {
                return modele.execute(statut -> traitement.get());
            }
        };
    }
}
