package sn.sunuagri.commercialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.sunuagri.commercialisation.application.CommandeService;
import sn.sunuagri.commercialisation.application.OffreService;
import sn.sunuagri.commercialisation.application.OffreService.PublicationOffre;
import sn.sunuagri.commercialisation.domaine.commande.Commande;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;
import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.commercialisation.domaine.offre.StatutOffre;
import sn.sunuagri.partage.TransactionsImmediates;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.PageDemandee;
import sn.sunuagri.partage.domaine.PageResultat;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;
import sn.sunuagri.stock.LotsEnMemoire;
import sn.sunuagri.stock.application.StockService;
import sn.sunuagri.stock.domaine.EtatStock;
import sn.sunuagri.stock.domaine.Lot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-09, US-10, US-11, US-12, US-16 et accès US-17. */
class CommercialisationTest {

    private final LotsEnMemoire lots = new LotsEnMemoire();
    private final OffresEnMemoire offres = new OffresEnMemoire();
    private final CommandesEnMemoire commandes = new CommandesEnMemoire();
    private final HorlogeReglable horloge = new HorlogeReglable();
    private final StockService stock = new StockService(lots, offres);
    private final OffreService offreService = new OffreService(offres, stock, OffreService.ControlePrix.AUCUN,
            new TransactionsImmediates(), horloge);
    private final CommandeService commandeService = new CommandeService(commandes, offres, ReglesCooperative.parDefaut(),
            new TransactionsImmediates(), horloge);
    private final Utilisateur agent = Utilisateur.agent();
    private final Utilisateur fatou = Utilisateur.acheteur("ACH-FATOU");
    private final Utilisateur ibou = Utilisateur.acheteur("ACH-IBOU");

    @BeforeEach
    void stockDeDepart() {
        lots.enregistrer(Lot.receptionner(UUID.randomUUID(), UUID.randomUUID(), TypeCulture.ARACHIDE, Qualite.A,
                Kilogrammes.de(1000), new BigDecimal("8"), Instant.parse("2026-09-01T10:00:00Z")));
    }

    private EtatStock etat() {
        return stock.etat(TypeCulture.ARACHIDE, Qualite.A);
    }

    private Offre offre(long kg) {
        return offreService.publier(agent, new PublicationOffre(TypeCulture.ARACHIDE, Qualite.A, kg, 350));
    }

    @Test
    void une_offre_ne_depasse_pas_le_stock_disponible_et_reserve_sa_quantite() {
        offre(600);
        assertThat(etat().reserve().valeur()).isEqualTo(600);
        assertThat(etat().disponible().valeur()).isEqualTo(400);

        assertThatThrownBy(() -> offre(401))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("STOCK_DISPONIBLE_INSUFFISANT");
    }

    @Test
    void le_prix_d_une_offre_est_strictement_positif() {
        assertThatThrownBy(() -> offreService.publier(agent, new PublicationOffre(TypeCulture.ARACHIDE, Qualite.A, 100, 0)))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("PRIX_NON_POSITIF");
    }

    @Test
    void retirer_une_offre_libere_la_quantite_non_vendue() {
        Offre o = offre(600);
        commandeService.passer(fatou, o.id(), 200);
        offreService.retirer(agent, o.id());

        assertThat(etat().reserve().valeur()).isEqualTo(200);
        assertThat(etat().disponible().valeur()).isEqualTo(800);
    }

    @Test
    void le_montant_est_la_quantite_multipliee_par_le_prix() {
        Commande c = commandeService.passer(fatou, offre(600).id(), 200);
        assertThat(c.montant().valeur()).isEqualTo(70_000);
        assertThat(c.statut()).isEqualTo(StatutCommande.EN_ATTENTE);
    }

    @Test
    void la_quantite_minimale_d_une_commande_est_de_50_kg() {
        Offre o = offre(600);
        assertThatThrownBy(() -> commandeService.passer(fatou, o.id(), 49))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("COMMANDE_MINIMALE_NON_ATTEINTE");
    }

    @Test
    void la_commande_ne_depasse_pas_le_restant_et_l_offre_s_epuise() {
        Offre o = offre(600);
        commandeService.passer(fatou, o.id(), 500);
        assertThatThrownBy(() -> commandeService.passer(ibou, o.id(), 101))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("QUANTITE_OFFRE_INSUFFISANTE");

        commandeService.passer(ibou, o.id(), 100);
        assertThat(offres.trouver(o.id()).orElseThrow().statut()).isEqualTo(StatutOffre.EPUISEE);
        assertThatThrownBy(() -> commandeService.passer(ibou, o.id(), 50)).isInstanceOf(RessourceIntrouvable.class);
    }

    @Test
    void annuler_rend_la_quantite_a_l_offre_qui_redevient_ouverte() {
        Offre o = offre(600);
        Commande c = commandeService.passer(fatou, o.id(), 600);
        commandeService.annuler(fatou, c.id());

        Offre apres = offres.trouver(o.id()).orElseThrow();
        assertThat(apres.statut()).isEqualTo(StatutOffre.OUVERTE);
        assertThat(apres.quantiteRestante().valeur()).isEqualTo(600);
        assertThat(etat().disponible().valeur()).isEqualTo(400);
    }

    @Test
    void annuler_une_commande_livree_est_refuse() {
        Offre o = offre(600);
        Commande c = commandeService.passer(fatou, o.id(), 100);
        c.livrer(o, Instant.now());
        commandes.enregistrer(c);

        assertThatThrownBy(() -> commandeService.annuler(fatou, c.id()))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("COMMANDE_DEJA_LIVREE");
    }

    @Test
    void un_acheteur_ne_peut_ni_voir_ni_annuler_la_commande_d_un_autre() {
        Commande c = commandeService.passer(fatou, offre(600).id(), 100);
        assertThatThrownBy(() -> commandeService.consulter(ibou, c.id())).isInstanceOf(RessourceIntrouvable.class);
        assertThatThrownBy(() -> commandeService.annuler(ibou, c.id())).isInstanceOf(RessourceIntrouvable.class);
    }

    @Test
    void l_historique_est_pagine_du_plus_recent_au_plus_ancien_filtre_et_limite_a_ses_commandes() {
        Offre o = offre(1000);
        Commande premiere = commandeService.passer(fatou, o.id(), 50);
        Commande deuxieme = commandeService.passer(fatou, o.id(), 60);
        Commande troisieme = commandeService.passer(fatou, o.id(), 70);
        commandeService.passer(ibou, o.id(), 80);
        commandeService.annuler(fatou, deuxieme.id());

        PageResultat<Commande> page0 = commandeService.historique(fatou, null, new PageDemandee(0, 2));
        assertThat(page0.elements()).extracting(Commande::id).containsExactly(troisieme.id(), deuxieme.id());
        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(commandeService.historique(fatou, null, new PageDemandee(1, 2)).elements())
                .extracting(Commande::id).containsExactly(premiere.id());
        assertThat(commandeService.historique(fatou, StatutCommande.ANNULEE, new PageDemandee(0, 10)).elements())
                .extracting(Commande::id).containsExactly(deuxieme.id());
    }

    @Test
    void un_acheteur_ne_voit_que_les_offres_ouvertes() {
        Offre ouverte = offre(300);
        Offre retiree = offre(300);
        offreService.retirer(agent, retiree.id());

        assertThat(offreService.lister(fatou, null)).extracting(Offre::id).containsExactly(ouverte.id());
        assertThatThrownBy(() -> offreService.consulter(fatou, retiree.id())).isInstanceOf(RessourceIntrouvable.class);
        assertThat(offreService.lister(agent, null)).hasSize(2);
    }
}
