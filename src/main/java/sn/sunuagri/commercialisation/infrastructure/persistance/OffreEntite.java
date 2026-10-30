package sn.sunuagri.commercialisation.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import sn.sunuagri.commercialisation.domaine.offre.StatutOffre;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offre", indexes = {
        @Index(name = "ix_offre_culture_qualite", columnList = "culture, qualite"),
        @Index(name = "ix_offre_statut", columnList = "statut")})
public class OffreEntite {

    @Id
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TypeCulture culture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    Qualite qualite;

    @Column(nullable = false)
    long quantiteKg;

    @Column(nullable = false)
    long prixParKg;

    @Column(nullable = false)
    Instant datePublication;

    @Column(length = 500)
    String avertissementPrix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    StatutOffre statut;

    @Column(nullable = false)
    long quantiteCommandeeKg;

    @Column(nullable = false)
    long quantiteLivreeKg;

    /** Copie de {@code Offre.quantiteReservee()} (calculée par le domaine) pour sommer en base. */
    @Column(nullable = false)
    long quantiteReserveeKg;
}
