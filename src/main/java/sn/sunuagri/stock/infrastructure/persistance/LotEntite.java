package sn.sunuagri.stock.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lot", indexes = @Index(name = "ix_lot_culture_qualite", columnList = "culture, qualite, dateReception"))
public class LotEntite {

    @Id
    UUID id;

    @Version
    Long version;

    @Column(nullable = false)
    UUID producteurId;

    @Column(nullable = false)
    UUID cultureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TypeCulture culture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    Qualite qualite;

    @Column(nullable = false)
    long quantiteInitialeKg;

    @Column(nullable = false)
    long quantiteRestanteKg;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal tauxHumidite;

    @Column(nullable = false)
    Instant dateReception;
}
