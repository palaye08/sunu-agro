package sn.sunuagri.partage;

import org.junit.jupiter.api.Test;
import sn.sunuagri.partage.domaine.Hectares;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjetsValeursTest {

    @Test
    void une_quantite_strictement_positive_refuse_zero() {
        assertThatThrownBy(() -> Kilogrammes.strictementPositive(0))
                .isInstanceOf(RegleMetierEnfreinte.class)
                .hasMessageContaining("strictement positive");
    }

    @Test
    void une_superficie_strictement_positive_refuse_les_valeurs_negatives() {
        assertThatThrownBy(() -> Hectares.strictementPositive(new BigDecimal("-1")))
                .isInstanceOf(RegleMetierEnfreinte.class);
    }

    @Test
    void deux_superficies_egales_en_valeur_sont_egales() {
        assertThat(Hectares.de("2.50")).isEqualTo(Hectares.de("2.5"));
    }

    @Test
    void le_montant_est_formate_en_francais() {
        assertThat(MontantXof.de(125000).enFrancais()).isEqualTo("125 000 FCFA");
    }

    @Test
    void un_taux_est_arrondi_au_franc_le_plus_proche() {
        assertThat(MontantXof.de(1010).multiplierParTaux(new BigDecimal("0.05"))).isEqualTo(MontantXof.de(51));
    }
}
