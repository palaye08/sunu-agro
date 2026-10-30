package sn.sunuagri.stock.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;

import java.util.UUID;

/** Quantité prélevée dans un lot lors de la livraison d'une commande. */
public record Prelevement(UUID lotId, UUID producteurId, Kilogrammes quantite) {
}
