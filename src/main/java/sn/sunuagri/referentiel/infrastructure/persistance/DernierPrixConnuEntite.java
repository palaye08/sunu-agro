package sn.sunuagri.referentiel.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.time.Instant;

@Entity
@Table(name = "dernier_prix_connu")
public class DernierPrixConnuEntite {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    TypeCulture culture;

    @Column(nullable = false)
    long prixParKg;

    @Column(nullable = false)
    Instant date;
}
