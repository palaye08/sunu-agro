package sn.sunuagri.stock;

import org.junit.jupiter.api.Test;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.stock.domaine.ConsommationLots;
import sn.sunuagri.stock.domaine.EtatStock;
import sn.sunuagri.stock.domaine.Lot;
import sn.sunuagri.stock.domaine.Prelevement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-09 (disponible) et US-13 (consommation FIFO). */
class ConsommationLotsTest {

    private static final UUID AWA = UUID.randomUUID();
    private static final UUID MODOU = UUID.randomUUID();

    private Lot lot(UUID producteur, long kg, String date) {
        return Lot.receptionner(producteur, UUID.randomUUID(), TypeCulture.ARACHIDE, Qualite.A, Kilogrammes.de(kg),
                new BigDecimal("8"), Instant.parse(date));
    }

    @Test
    void les_lots_sont_consommes_du_plus_ancien_au_plus_recent() {
        Lot recent = lot(MODOU, 500, "2026-10-20T10:00:00Z");
        Lot ancien = lot(AWA, 300, "2026-10-10T10:00:00Z");

        List<Prelevement> prelevements = ConsommationLots.consommer(List.of(recent, ancien), Kilogrammes.de(600));

        assertThat(prelevements).extracting(Prelevement::producteurId).containsExactly(AWA, MODOU);
        assertThat(prelevements).extracting(p -> p.quantite().valeur()).containsExactly(300L, 300L);
        assertThat(ancien.estEpuise()).isTrue();
        assertThat(recent.quantiteRestante().valeur()).isEqualTo(200);
    }

    @Test
    void un_stock_physique_insuffisant_est_refuse_sans_rien_consommer() {
        Lot l = lot(AWA, 100, "2026-10-10T10:00:00Z");
        assertThatThrownBy(() -> ConsommationLots.consommer(List.of(l), Kilogrammes.de(101)))
                .isInstanceOf(RegleMetierEnfreinte.class).extracting("code").isEqualTo("STOCK_INSUFFISANT");
        assertThat(l.quantiteRestante().valeur()).isEqualTo(100);
    }

    @Test
    void le_disponible_est_le_total_moins_le_reserve() {
        EtatStock etat = new EtatStock(TypeCulture.ARACHIDE, Qualite.A, Kilogrammes.de(1000), Kilogrammes.de(300));
        assertThat(etat.disponible().valeur()).isEqualTo(700);
    }
}
