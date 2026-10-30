package sn.sunuagri.referentiel.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.partage.domaine.Hectares;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.referentiel.application.ParametresCultureService;
import sn.sunuagri.referentiel.domaine.ParametresCulture;
import sn.sunuagri.referentiel.domaine.ParametresCultureRepository;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;

@Configuration
public class ConfigurationReferentiel {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationReferentiel.class);

    @Bean
    ReglesCooperative reglesCooperative(ProprietesReferentiel proprietes) {
        ProprietesReferentiel.Cooperative c = proprietes.cooperative();
        return new ReglesCooperative(new Hectares(c.superficieMaxParProducteurHa()),
                Kilogrammes.de(c.quantiteMinCommandeKg()), c.tauxCommission(), c.ecartPrixAvertissement());
    }

    @Bean
    ParametresCultureService parametresCultureService(ParametresCultureRepository repository, Transactions transactions) {
        return new ParametresCultureService(repository, transactions);
    }

    /** Au premier démarrage, charge les paramètres de référence ; ensuite, la base fait foi (US-19). */
    @Bean
    ApplicationRunner initialiserParametresCultures(ProprietesReferentiel proprietes,
                                                    ParametresCultureRepository repository,
                                                    Transactions transactions) {
        return args -> transactions.executer(() -> proprietes.cultures().forEach((type, valeurs) -> {
            if (repository.trouver(type).isEmpty()) {
                repository.enregistrer(new ParametresCulture(type, valeurs.rendementReferenceKgHa(),
                        valeurs.humiditeMaxPourcent(), valeurs.cycleMinimalJours()));
                LOG.info("Paramètres initiaux chargés pour la culture {}", type);
            }
        }));
    }
}
