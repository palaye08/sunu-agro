package sn.sunuagri.paiement.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;

import java.util.List;
import java.util.UUID;

/** Résultat de la répartition du montant d'une commande. */
public record Repartition(MontantXof montantCommande, MontantXof commission, List<Part> parts) {

    public record Part(UUID producteurId, Kilogrammes quantite, MontantXof montant) {
    }

    public MontantXof totalVerse() {
        return parts.stream().map(Part::montant).reduce(MontantXof.ZERO, MontantXof::plus);
    }
}
