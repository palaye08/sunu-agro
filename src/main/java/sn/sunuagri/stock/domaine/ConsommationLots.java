package sn.sunuagri.stock.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service de domaine (US-13) : consomme les lots du plus ancien au plus récent (FIFO).
 * En cas d'égalité de date de réception, l'identifiant départage pour rester déterministe.
 */
public final class ConsommationLots {

    private ConsommationLots() {
    }

    public static List<Prelevement> consommer(List<Lot> lots, Kilogrammes quantite) {
        List<Lot> parAnciennete = lots.stream()
                .filter(l -> !l.estEpuise())
                .sorted(Comparator.comparing(Lot::dateReception).thenComparing(Lot::id))
                .toList();
        Kilogrammes disponible = parAnciennete.stream().map(Lot::quantiteRestante).reduce(Kilogrammes.ZERO, Kilogrammes::plus);
        RegleMetierEnfreinte.verifier(!quantite.depasse(disponible), "STOCK_INSUFFISANT",
                "Stock physique insuffisant : " + quantite + " demandés, " + disponible + " présents dans les lots.");

        List<Prelevement> prelevements = new ArrayList<>();
        Kilogrammes reste = quantite;
        for (Lot lot : parAnciennete) {
            if (reste.estNul()) {
                break;
            }
            Kilogrammes preleve = lot.prelever(reste);
            prelevements.add(new Prelevement(lot.id(), lot.producteurId(), preleve));
            reste = reste.moins(preleve);
        }
        return prelevements;
    }
}
