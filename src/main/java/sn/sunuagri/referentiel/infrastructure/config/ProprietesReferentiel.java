package sn.sunuagri.referentiel.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/** Valeurs de référence lues dans application.properties (préfixe {@code sunuagri}). */
@ConfigurationProperties(prefix = "sunuagri")
public record ProprietesReferentiel(Cooperative cooperative, Map<TypeCulture, Culture> cultures) {

    public ProprietesReferentiel {
        cultures = cultures == null ? new EnumMap<>(TypeCulture.class) : cultures;
    }

    public record Cooperative(BigDecimal superficieMaxParProducteurHa, long quantiteMinCommandeKg,
                              BigDecimal tauxCommission, BigDecimal ecartPrixAvertissement) {
    }

    public record Culture(BigDecimal rendementReferenceKgHa, BigDecimal humiditeMaxPourcent, int cycleMinimalJours) {
    }
}
