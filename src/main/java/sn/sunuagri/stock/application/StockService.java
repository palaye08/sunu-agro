package sn.sunuagri.stock.application;

import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.stock.domaine.EtatStock;
import sn.sunuagri.stock.domaine.LotRepository;
import sn.sunuagri.stock.domaine.ReservationsPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Cas d'usage US-09 : consulter le stock par culture et par qualité. */
public class StockService {

    private final LotRepository lots;
    private final ReservationsPort reservations;

    public StockService(LotRepository lots, ReservationsPort reservations) {
        this.lots = lots;
        this.reservations = reservations;
    }

    /** Filtres facultatifs : sans filtre, toutes les combinaisons culture × qualité sont rendues. */
    public List<EtatStock> consulter(Utilisateur utilisateur, TypeCulture culture, Qualite qualite) {
        utilisateur.exigerRole(Role.AGENT);
        List<EtatStock> etats = new ArrayList<>();
        for (TypeCulture c : culture == null ? Arrays.asList(TypeCulture.values()) : List.of(culture)) {
            for (Qualite q : qualite == null ? Arrays.asList(Qualite.values()) : List.of(qualite)) {
                etats.add(etat(c, q));
            }
        }
        return etats;
    }

    /** État calculé à la demande : toujours cohérent après livraison, vente ou annulation. */
    public EtatStock etat(TypeCulture culture, Qualite qualite) {
        return new EtatStock(culture, qualite, lots.quantitePresente(culture, qualite),
                reservations.quantiteReservee(culture, qualite));
    }
}
