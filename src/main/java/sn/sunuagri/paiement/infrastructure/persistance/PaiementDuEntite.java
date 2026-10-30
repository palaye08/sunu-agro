package sn.sunuagri.paiement.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import sn.sunuagri.paiement.domaine.StatutNotification;
import sn.sunuagri.paiement.domaine.StatutPaiement;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paiement_du", indexes = {
        @Index(name = "ix_paiement_producteur", columnList = "producteurId"),
        @Index(name = "ix_paiement_commande", columnList = "commandeId")})
public class PaiementDuEntite {

    @Id
    UUID id;

    @Column(nullable = false)
    UUID commandeId;

    @Column(nullable = false)
    UUID producteurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TypeCulture culture;

    @Column(nullable = false)
    long quantiteKg;

    @Column(nullable = false)
    long montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    StatutPaiement statut;

    @Column(nullable = false)
    Instant dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    StatutNotification notification;

    Instant dateNotification;

    @Column(length = 500)
    String motifEchecNotification;
}
