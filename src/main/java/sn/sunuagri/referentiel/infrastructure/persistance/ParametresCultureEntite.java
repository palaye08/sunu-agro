package sn.sunuagri.referentiel.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.math.BigDecimal;

@Entity
@Table(name = "parametres_culture")
public class ParametresCultureEntite {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    TypeCulture culture;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal rendementReferenceKgParHa;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal humiditeMaxPourcent;

    @Column(nullable = false)
    int cycleMinimalJours;
}
