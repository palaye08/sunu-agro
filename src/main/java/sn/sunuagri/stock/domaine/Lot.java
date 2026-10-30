package sn.sunuagri.stock.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat Lot : quantité reçue d'un producteur à l'entrepôt, avec sa qualité et son humidité.
 * Créé par une livraison acceptée (US-08), consommé par les livraisons de commandes (US-13).
 */
public class Lot {

    private final UUID id;
    private final UUID producteurId;
    private final UUID cultureId;
    private final TypeCulture culture;
    private final Qualite qualite;
    private final Kilogrammes quantiteInitiale;
    private Kilogrammes quantiteRestante;
    private final BigDecimal tauxHumidite;
    private final Instant dateReception;

    private Lot(UUID id, UUID producteurId, UUID cultureId, TypeCulture culture, Qualite qualite,
                Kilogrammes quantiteInitiale, Kilogrammes quantiteRestante, BigDecimal tauxHumidite, Instant dateReception) {
        this.id = Objects.requireNonNull(id);
        this.producteurId = Objects.requireNonNull(producteurId);
        this.cultureId = cultureId;
        this.culture = Objects.requireNonNull(culture);
        this.qualite = qualite;
        this.quantiteInitiale = quantiteInitiale;
        this.quantiteRestante = quantiteRestante;
        this.tauxHumidite = tauxHumidite;
        this.dateReception = Objects.requireNonNull(dateReception);
    }

    /** Réception d'une livraison acceptée : le lot est rattaché au producteur et daté. */
    public static Lot receptionner(UUID producteurId, UUID cultureId, TypeCulture culture, Qualite qualite,
                                   Kilogrammes quantite, BigDecimal tauxHumidite, Instant dateReception) {
        RegleMetierEnfreinte.verifier(qualite != null, "QUALITE_OBLIGATOIRE", "La qualité (A, B ou C) est obligatoire.");
        RegleMetierEnfreinte.verifier(!quantite.estNul(), "QUANTITE_NON_POSITIVE", "La quantité doit être strictement positive.");
        return new Lot(UUID.randomUUID(), producteurId, cultureId, culture, qualite, quantite, quantite,
                tauxHumidite, dateReception);
    }

    public static Lot reconstituer(UUID id, UUID producteurId, UUID cultureId, TypeCulture culture, Qualite qualite,
                                   Kilogrammes quantiteInitiale, Kilogrammes quantiteRestante, BigDecimal tauxHumidite,
                                   Instant dateReception) {
        return new Lot(id, producteurId, cultureId, culture, qualite, quantiteInitiale, quantiteRestante,
                tauxHumidite, dateReception);
    }

    /** Prélève au plus {@code demande} dans le lot et retourne la quantité effectivement prélevée. */
    Kilogrammes prelever(Kilogrammes demande) {
        Kilogrammes preleve = quantiteRestante.min(demande);
        quantiteRestante = quantiteRestante.moins(preleve);
        return preleve;
    }

    public boolean estEpuise() {
        return quantiteRestante.estNul();
    }

    public UUID id() {
        return id;
    }

    public UUID producteurId() {
        return producteurId;
    }

    public UUID cultureId() {
        return cultureId;
    }

    public TypeCulture culture() {
        return culture;
    }

    public Qualite qualite() {
        return qualite;
    }

    public Kilogrammes quantiteInitiale() {
        return quantiteInitiale;
    }

    public Kilogrammes quantiteRestante() {
        return quantiteRestante;
    }

    public BigDecimal tauxHumidite() {
        return tauxHumidite;
    }

    public Instant dateReception() {
        return dateReception;
    }
}
