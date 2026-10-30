package sn.sunuagri.stock.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

/**
 * Port de sortie : quantité réservée par les offres de vente.
 * Défini par le Stock, implémenté par la Commercialisation (inversion de dépendance).
 */
public interface ReservationsPort {

    Kilogrammes quantiteReservee(TypeCulture culture, Qualite qualite);
}
