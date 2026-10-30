package sn.sunuagri.commercialisation.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commande", indexes = {
        @Index(name = "ix_commande_acheteur_date", columnList = "acheteurId, dateCreation"),
        @Index(name = "ix_commande_acheteur_statut_date", columnList = "acheteurId, statut, dateCreation")})
public class CommandeEntite {

    @Id
    UUID id;

    @Column(nullable = false)
    UUID offreId;

    @Column(nullable = false, length = 100)
    String acheteurId;

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
    long montant;

    @Column(nullable = false)
    Instant dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    StatutCommande statut;

    Instant dateLivraison;
}
