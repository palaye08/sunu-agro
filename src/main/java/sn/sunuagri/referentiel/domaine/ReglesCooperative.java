package sn.sunuagri.referentiel.domaine;

import sn.sunuagri.partage.domaine.Hectares;
import sn.sunuagri.partage.domaine.Kilogrammes;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Valeurs de référence de la coopérative. Elles ne sont écrites qu'ici
 * (et dans la configuration), jamais en dur dans les règles métier.
 */
public record ReglesCooperative(
        Hectares superficieMaxParProducteur,
        Kilogrammes quantiteMinimaleCommande,
        BigDecimal tauxCommission,
        BigDecimal ecartPrixAvertissement) {

    public ReglesCooperative {
        Objects.requireNonNull(superficieMaxParProducteur);
        Objects.requireNonNull(quantiteMinimaleCommande);
        Objects.requireNonNull(tauxCommission);
        Objects.requireNonNull(ecartPrixAvertissement);
        if (tauxCommission.signum() < 0 || tauxCommission.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Le taux de commission doit être compris entre 0 et 1.");
        }
    }

    /** Valeurs du sujet, utiles aux tests. */
    public static ReglesCooperative parDefaut() {
        return new ReglesCooperative(Hectares.de("50"), Kilogrammes.de(50),
                new BigDecimal("0.05"), new BigDecimal("0.30"));
    }
}
