package sn.sunuagri.referentiel;

import org.junit.jupiter.api.Test;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.application.FournisseurPrixMarche;
import sn.sunuagri.referentiel.application.PrixMarcheService;
import sn.sunuagri.referentiel.application.PrixMarcheService.Source;
import sn.sunuagri.referentiel.domaine.DernierPrixConnuRepository;
import sn.sunuagri.referentiel.domaine.PrixReference;
import sn.sunuagri.referentiel.domaine.ReglesCooperative;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** US-18. */
class PrixMarcheServiceTest {

    private static final Instant HIER = Instant.parse("2026-10-29T09:00:00Z");

    private boolean disponible = true;
    private final FournisseurPrixMarche fournisseur = culture -> {
        if (!disponible) {
            throw new FournisseurPrixMarche.ServiceIndisponible("panne");
        }
        return new PrixReference(culture, MontantXof.de(350), HIER);
    };
    private final Map<TypeCulture, PrixReference> memoire = new EnumMap<>(TypeCulture.class);
    private final DernierPrixConnuRepository derniers = new DernierPrixConnuRepository() {
        public Optional<PrixReference> trouver(TypeCulture c) {
            return Optional.ofNullable(memoire.get(c));
        }

        public void enregistrer(PrixReference p) {
            memoire.put(p.culture(), p);
        }
    };
    private final PrixMarcheService service = new PrixMarcheService(fournisseur, derniers, ReglesCooperative.parDefaut());

    @Test
    void si_le_service_est_indisponible_le_dernier_prix_connu_est_rendu_avec_sa_date() {
        service.consulter(Utilisateur.agent(), TypeCulture.ARACHIDE);
        disponible = false;

        PrixMarcheService.PrixMarche prix = service.consulter(Utilisateur.agent(), TypeCulture.ARACHIDE);
        assertThat(prix.source()).isEqualTo(Source.DERNIER_PRIX_CONNU);
        assertThat(prix.prix().date()).isEqualTo(HIER);
        assertThat(prix.prix().prixParKg().valeur()).isEqualTo(350);
    }

    @Test
    void sans_service_ni_prix_connu_l_erreur_est_explicite_et_l_offre_reste_possible() {
        disponible = false;
        assertThatThrownBy(() -> service.consulter(Utilisateur.agent(), TypeCulture.MIL))
                .isInstanceOf(PrixMarcheService.PrixMarcheIndisponible.class);
        assertThat(service.avertissementPourOffre(TypeCulture.MIL, 1000)).isNull();
    }

    @Test
    void un_prix_ecarte_de_plus_de_30_pourcent_donne_un_avertissement() {
        assertThat(service.avertissementPourOffre(TypeCulture.ARACHIDE, 455)).isNull();
        assertThat(service.avertissementPourOffre(TypeCulture.ARACHIDE, 245)).isNull();
        assertThat(service.avertissementPourOffre(TypeCulture.ARACHIDE, 456)).contains("30,3 %".replace(',', '.'));
        assertThat(service.avertissementPourOffre(TypeCulture.ARACHIDE, 200)).contains("42.9 %");
    }
}
