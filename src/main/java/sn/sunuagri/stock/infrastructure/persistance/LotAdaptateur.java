package sn.sunuagri.stock.infrastructure.persistance;

import org.springframework.stereotype.Repository;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.stock.domaine.Lot;
import sn.sunuagri.stock.domaine.LotRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class LotAdaptateur implements LotRepository {

    private final LotJpa jpa;

    LotAdaptateur(LotJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Lot> trouver(UUID id) {
        return jpa.findById(id).map(LotAdaptateur::versDomaine);
    }

    @Override
    public List<Lot> nonEpuises(TypeCulture culture, Qualite qualite) {
        return jpa.findByCultureAndQualiteAndQuantiteRestanteKgGreaterThan(culture, qualite, 0).stream()
                .map(LotAdaptateur::versDomaine).toList();
    }

    @Override
    public Kilogrammes quantitePresente(TypeCulture culture, Qualite qualite) {
        return Kilogrammes.de(jpa.sommeRestante(culture, qualite));
    }

    @Override
    public void enregistrer(Lot lot) {
        LotEntite e = jpa.findById(lot.id()).orElseGet(LotEntite::new);
        e.id = lot.id();
        e.producteurId = lot.producteurId();
        e.cultureId = lot.cultureId();
        e.culture = lot.culture();
        e.qualite = lot.qualite();
        e.quantiteInitialeKg = lot.quantiteInitiale().valeur();
        e.quantiteRestanteKg = lot.quantiteRestante().valeur();
        e.tauxHumidite = lot.tauxHumidite();
        e.dateReception = lot.dateReception();
        jpa.save(e);
    }

    private static Lot versDomaine(LotEntite e) {
        return Lot.reconstituer(e.id, e.producteurId, e.cultureId, e.culture, e.qualite,
                Kilogrammes.de(e.quantiteInitialeKg), Kilogrammes.de(e.quantiteRestanteKg), e.tauxHumidite, e.dateReception);
    }
}
