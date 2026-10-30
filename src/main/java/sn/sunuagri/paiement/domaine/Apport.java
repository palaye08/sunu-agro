package sn.sunuagri.paiement.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;

import java.util.UUID;

/** Quantité qu'un producteur a apportée dans les lots consommés par une commande. */
public record Apport(UUID producteurId, Kilogrammes quantite) {
}
