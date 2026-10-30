package sn.sunuagri.paiement;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import sn.sunuagri.paiement.domaine.Apport;
import sn.sunuagri.paiement.domaine.Repartition;
import sn.sunuagri.paiement.domaine.RepartitionPaiement;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** US-13 : commission de 5 % puis répartition proportionnelle, sans perte ni création de franc. */
class RepartitionPaiementTest {

    private static final BigDecimal CINQ_POURCENT = new BigDecimal("0.05");
    private final UUID awa = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID modou = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private final UUID fatou = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    private static Apport apport(UUID producteur, long kg) {
        return new Apport(producteur, Kilogrammes.de(kg));
    }

    private static long montantDe(Repartition r, UUID producteur) {
        return r.parts().stream().filter(p -> p.producteurId().equals(producteur)).findFirst().orElseThrow().montant().valeur();
    }

    @Test
    void la_cooperative_retient_5_pourcent_puis_repartit_au_prorata() {
        // 600 kg x 350 = 210 000 ; commission 10 500 ; à répartir 199 500
        Repartition r = RepartitionPaiement.repartir(MontantXof.de(210_000), CINQ_POURCENT,
                List.of(apport(awa, 400), apport(modou, 200)));

        assertThat(r.commission().valeur()).isEqualTo(10_500);
        assertThat(montantDe(r, awa)).isEqualTo(133_000);
        assertThat(montantDe(r, modou)).isEqualTo(66_500);
    }

    @Test
    void les_apports_d_un_meme_producteur_sont_cumules() {
        Repartition r = RepartitionPaiement.repartir(MontantXof.de(10_000), CINQ_POURCENT,
                List.of(apport(awa, 100), apport(modou, 100), apport(awa, 200)));
        assertThat(r.parts()).hasSize(2);
        assertThat(montantDe(r, awa)).isEqualTo(7_125);
        assertThat(montantDe(r, modou)).isEqualTo(2_375);
    }

    @Test
    void les_francs_restants_vont_aux_plus_forts_restes() {
        // 100 F : commission 5 F ; 95 F à répartir en trois parts égales -> 31,67 chacun
        Repartition r = RepartitionPaiement.repartir(MontantXof.de(100), CINQ_POURCENT,
                List.of(apport(awa, 1), apport(modou, 1), apport(fatou, 1)));

        assertThat(r.parts()).extracting(p -> p.montant().valeur()).containsExactlyInAnyOrder(32L, 32L, 31L);
        assertThat(r.totalVerse().plus(r.commission()).valeur()).isEqualTo(100);
    }

    @RepeatedTest(200)
    void aucun_franc_ne_se_perd_ni_n_apparait() {
        Random hasard = new Random();
        List<Apport> apports = new ArrayList<>();
        for (int i = 0; i < 1 + hasard.nextInt(8); i++) {
            apports.add(apport(UUID.randomUUID(), 1 + hasard.nextInt(5_000)));
        }
        long montant = 1 + hasard.nextLong(50_000_000);

        Repartition r = RepartitionPaiement.repartir(MontantXof.de(montant), CINQ_POURCENT, apports);

        assertThat(r.totalVerse().valeur() + r.commission().valeur()).isEqualTo(montant);
    }
}
