package sn.sunuagri.stock.application;

import sn.sunuagri.exploitation.domaine.culture.Culture;
import sn.sunuagri.exploitation.domaine.culture.CultureRepository;
import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;
import sn.sunuagri.stock.domaine.Lot;
import sn.sunuagri.stock.domaine.LotRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/**
 * Cas d'usage US-08 : livrer une récolte à l'entrepôt.
 * La culture vérifie ses règles (humidité, cumul) puis un lot est créé, dans une même transaction :
 * une livraison refusée ne modifie rien.
 */
public class LivraisonEntrepotService {

    private final CultureRepository cultures;
    private final LotRepository lots;
    private final Transactions transactions;
    private final Clock horloge;

    public LivraisonEntrepotService(CultureRepository cultures, LotRepository lots, Transactions transactions, Clock horloge) {
        this.cultures = cultures;
        this.lots = lots;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public record LivraisonEntrepot(long quantiteKg, BigDecimal tauxHumidite, Qualite qualite) {
    }

    public Lot livrer(Utilisateur utilisateur, UUID cultureId, LivraisonEntrepot demande) {
        utilisateur.exigerRole(Role.AGENT);
        return transactions.executer(() -> {
            Culture culture = cultures.trouver(cultureId)
                    .orElseThrow(() -> new RessourceIntrouvable("culture", cultureId));
            culture.livrerALEntrepot(demande.quantiteKg(), demande.tauxHumidite());
            Lot lot = Lot.receptionner(culture.producteurId(), culture.id(), culture.type(), demande.qualite(),
                    Kilogrammes.de(demande.quantiteKg()), demande.tauxHumidite(), horloge.instant());
            cultures.enregistrer(culture);
            lots.enregistrer(lot);
            return lot;
        });
    }

    /** Un producteur ne voit que ses propres lots. */
    public Lot consulter(Utilisateur utilisateur, UUID lotId) {
        utilisateur.exigerRole(Role.AGENT, Role.PRODUCTEUR);
        return lots.trouver(lotId)
                .filter(l -> utilisateur.peutVoirDonneesDuProducteur(l.producteurId()))
                .orElseThrow(() -> new RessourceIntrouvable("lot", lotId));
    }
}
