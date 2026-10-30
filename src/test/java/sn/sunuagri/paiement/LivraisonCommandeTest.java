package sn.sunuagri.paiement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.sunuagri.commercialisation.CommandesEnMemoire;
import sn.sunuagri.commercialisation.HorlogeReglable;
import sn.sunuagri.commercialisation.OffresEnMemoire;
import sn.sunuagri.commercialisation.application.CommandeService;
import sn.sunuagri.commercialisation.application.LivraisonCommandeService;
import sn.sunuagri.commercialisation.application.LivraisonCommandeService.LivraisonCommande;
import sn.sunuagri.commercialisation.application.OffreService;
import sn.sunuagri.commercialisation.domaine.commande.Commande;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;
import sn.sunuagri.paiement.application.EnvoiSms;
import sn.sunuagri.paiement.application.NotificationPaiementService;
import sn.sunuagri.paiement.application.PaiementService;
import sn.sunuagri.paiement.domaine.PaiementDu;
import sn.sunuagri.paiement.domaine.StatutNotification;
import sn.sunuagri.paiement.domaine.StatutPaiement;
import sn.sunuagri.partage.TransactionsImmediates;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;
import sn.sunuagri.stock.LotsEnMemoire;
import sn.sunuagri.stock.application.StockService;
import sn.sunuagri.stock.domaine.Lot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-13 (livraison et répartition) et US-14 (SMS). */
class LivraisonCommandeTest {

    private final UUID awa = UUID.randomUUID();
    private final UUID modou = UUID.randomUUID();
    private final Map<UUID, String> telephones = Map.of(awa, "+221771234567", modou, "+221770000000");

    private final LotsEnMemoire lots = new LotsEnMemoire();
    private final OffresEnMemoire offres = new OffresEnMemoire();
    private final CommandesEnMemoire commandes = new CommandesEnMemoire();
    private final PaiementsEnMemoire paiements = new PaiementsEnMemoire();
    private final HorlogeReglable horloge = new HorlogeReglable();
    private final SmsEspion sms = new SmsEspion("+221770000000");
    private final TransactionsImmediates tx = new TransactionsImmediates();

    private final OffreService offreService = new OffreService(offres, new StockService(lots, offres),
            OffreService.ControlePrix.AUCUN, tx, horloge);
    private final CommandeService commandeService = new CommandeService(commandes, offres, ReglesCooperative.parDefaut(), tx, horloge);
    private final NotificationPaiementService notifications = new NotificationPaiementService(paiements,
            id -> Optional.ofNullable(telephones.get(id)), sms, tx, horloge);
    private final LivraisonCommandeService livraison = new LivraisonCommandeService(commandes, offres, lots,
            new PaiementService(paiements, ReglesCooperative.parDefaut(), horloge), notifications, tx, horloge);

    private final Utilisateur agent = Utilisateur.agent();
    private final Utilisateur acheteur = Utilisateur.acheteur("ACH-1");
    private Lot lotAwa;
    private Lot lotModou;

    @BeforeEach
    void stock() {
        lotModou = Lot.receptionner(modou, UUID.randomUUID(), TypeCulture.ARACHIDE, Qualite.A, Kilogrammes.de(500),
                new BigDecimal("8"), Instant.parse("2026-09-20T10:00:00Z"));
        lotAwa = Lot.receptionner(awa, UUID.randomUUID(), TypeCulture.ARACHIDE, Qualite.A, Kilogrammes.de(400),
                new BigDecimal("8"), Instant.parse("2026-09-10T10:00:00Z"));
        lots.enregistrer(lotModou);
        lots.enregistrer(lotAwa);
    }

    private Commande commande(long kg) {
        var offre = offreService.publier(agent, new OffreService.PublicationOffre(TypeCulture.ARACHIDE, Qualite.A, 900, 350));
        return commandeService.passer(acheteur, offre.id(), kg);
    }

    @Test
    void la_livraison_consomme_les_lots_les_plus_anciens_et_repartit_le_montant() {
        LivraisonCommande l = livraison.livrer(agent, commande(600).id());

        assertThat(l.commande().statut()).isEqualTo(StatutCommande.LIVREE);
        assertThat(l.lotsConsommes()).extracting(p -> p.producteurId()).containsExactly(awa, modou);
        assertThat(l.lotsConsommes()).extracting(p -> p.quantite().valeur()).containsExactly(400L, 200L);
        assertThat(lots.trouver(lotAwa.id()).orElseThrow().estEpuise()).isTrue();
        assertThat(lots.trouver(lotModou.id()).orElseThrow().quantiteRestante().valeur()).isEqualTo(300);

        // 210 000 F : commission 10 500 F ; 199 500 F répartis 2/3 - 1/3
        assertThat(l.repartition().commission().valeur()).isEqualTo(10_500);
        assertThat(l.paiements()).extracting(p -> p.montant().valeur()).containsExactlyInAnyOrder(133_000L, 66_500L);
        assertThat(l.paiements()).allMatch(p -> p.statut() == StatutPaiement.A_PAYER);
        assertThat(l.repartition().totalVerse().plus(l.repartition().commission())).isEqualTo(l.commande().montant());
    }

    @Test
    void le_stock_reste_coherent_apres_la_livraison() {
        livraison.livrer(agent, commande(600).id());
        var etat = new StockService(lots, offres).etat(TypeCulture.ARACHIDE, Qualite.A);
        assertThat(etat.total().valeur()).isEqualTo(300);
        assertThat(etat.reserve().valeur()).isEqualTo(300);
        assertThat(etat.disponible().valeur()).isZero();
    }

    @Test
    void une_commande_deja_livree_ou_annulee_ne_peut_pas_etre_livree() {
        Commande livree = commande(100);
        livraison.livrer(agent, livree.id());
        assertThatThrownBy(() -> livraison.livrer(agent, livree.id()))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("COMMANDE_NON_LIVRABLE");

        Commande annulee = commandeService.passer(acheteur, livree.offreId(), 100);
        commandeService.annuler(acheteur, annulee.id());
        assertThatThrownBy(() -> livraison.livrer(agent, annulee.id()))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("COMMANDE_NON_LIVRABLE");
        assertThat(paiements.tous()).hasSize(1);
    }

    @Test
    void un_sms_en_francais_indiquant_le_montant_est_envoye_au_producteur() {
        livraison.livrer(agent, commande(300).id());

        assertThat(sms.envoyes).hasSize(1);
        assertThat(sms.envoyes.get(0)).isEqualTo("+221771234567 : SunuAgri : un paiement de 99 750 FCFA vous est dû "
                + "pour la vente de 300 kg d'arachide. Statut : à payer.");
    }

    @Test
    void un_echec_d_envoi_ne_remet_pas_en_cause_la_livraison_et_est_trace() {
        LivraisonCommande l = livraison.livrer(agent, commande(600).id());

        PaiementDu paiementModou = l.paiements().stream().filter(p -> p.producteurId().equals(modou)).findFirst().orElseThrow();
        assertThat(paiementModou.notification()).isEqualTo(StatutNotification.ECHEC);
        assertThat(paiementModou.motifEchecNotification()).contains("injoignable");
        assertThat(commandes.trouver(l.commande().id()).orElseThrow().statut()).isEqualTo(StatutCommande.LIVREE);
    }

    @Test
    void un_sms_n_est_envoye_qu_une_seule_fois_par_paiement() {
        LivraisonCommande l = livraison.livrer(agent, commande(300).id());
        notifications.notifier(l.paiements().get(0).id());
        notifications.notifier(l.paiements().get(0).id());

        assertThat(sms.envoyes).hasSize(1);
    }

    static class SmsEspion implements EnvoiSms {
        final List<String> envoyes = new ArrayList<>();
        private final String numeroEnPanne;

        SmsEspion(String numeroEnPanne) {
            this.numeroEnPanne = numeroEnPanne;
        }

        @Override
        public void envoyer(String numero, String message) {
            if (numero.equals(numeroEnPanne)) {
                throw new EchecEnvoiSms("Opérateur injoignable");
            }
            envoyes.add(numero + " : " + message);
        }
    }
}
