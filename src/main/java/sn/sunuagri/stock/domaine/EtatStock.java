package sn.sunuagri.stock.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

/**
 * État du stock pour une culture et une qualité (US-09).
 * Total = quantité physiquement présente dans les lots ; réservé = engagé dans des offres ;
 * disponible = total − réservé.
 */
public record EtatStock(TypeCulture culture, Qualite qualite, Kilogrammes total, Kilogrammes reserve) {

    public Kilogrammes disponible() {
        return total.compareTo(reserve) >= 0 ? total.moins(reserve) : Kilogrammes.ZERO;
    }
}
