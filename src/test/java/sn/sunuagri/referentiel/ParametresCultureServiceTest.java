package sn.sunuagri.referentiel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.sunuagri.partage.TransactionsImmediates;
import sn.sunuagri.partage.application.AccesRefuse;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.application.ParametresCultureService;
import sn.sunuagri.referentiel.domaine.ParametresCulture;
import sn.sunuagri.referentiel.domaine.ParametresCultureRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParametresCultureServiceTest {

    private ParametresCultureService service;

    @BeforeEach
    void preparer() {
        service = new ParametresCultureService(ParametresEnMemoire.valeursDuSujet(), new TransactionsImmediates());
    }

    @Test
    void l_administrateur_modifie_les_parametres_d_une_culture() {
        ParametresCulture modifies = service.modifier(Utilisateur.administrateur(), TypeCulture.MIL,
                new BigDecimal("900"), new BigDecimal("13"), 80);

        assertThat(service.parametresActuels(TypeCulture.MIL)).isEqualTo(modifies);
        assertThat(modifies.cycleMinimalJours()).isEqualTo(80);
    }

    @Test
    void les_valeurs_doivent_etre_strictement_positives() {
        assertThatThrownBy(() -> service.modifier(Utilisateur.administrateur(), TypeCulture.MIL,
                BigDecimal.ZERO, new BigDecimal("13"), 80))
                .isInstanceOf(RegleMetierEnfreinte.class);
        assertThatThrownBy(() -> service.modifier(Utilisateur.administrateur(), TypeCulture.MIL,
                new BigDecimal("800"), new BigDecimal("13"), 0))
                .isInstanceOf(RegleMetierEnfreinte.class);
    }

    @Test
    void un_agent_ne_peut_pas_modifier_les_parametres() {
        assertThatThrownBy(() -> service.modifier(Utilisateur.agent(), TypeCulture.MIL,
                new BigDecimal("900"), new BigDecimal("13"), 80))
                .isInstanceOf(AccesRefuse.class);
    }
}
