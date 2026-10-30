package sn.sunuagri.referentiel.infrastructure.persistance;

import org.springframework.stereotype.Repository;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.ParametresCulture;
import sn.sunuagri.referentiel.domaine.ParametresCultureRepository;

import java.util.List;
import java.util.Optional;

/** Adaptateur de sortie JPA du port {@link ParametresCultureRepository}. */
@Repository
class ParametresCultureAdaptateur implements ParametresCultureRepository {

    private final ParametresCultureJpa jpa;

    ParametresCultureAdaptateur(ParametresCultureJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ParametresCulture> trouver(TypeCulture culture) {
        return jpa.findById(culture).map(ParametresCultureAdaptateur::versDomaine);
    }

    @Override
    public List<ParametresCulture> tous() {
        return jpa.findAll().stream().map(ParametresCultureAdaptateur::versDomaine).toList();
    }

    @Override
    public void enregistrer(ParametresCulture p) {
        ParametresCultureEntite e = new ParametresCultureEntite();
        e.culture = p.culture();
        e.rendementReferenceKgParHa = p.rendementReferenceKgParHa();
        e.humiditeMaxPourcent = p.humiditeMaxPourcent();
        e.cycleMinimalJours = p.cycleMinimalJours();
        jpa.save(e);
    }

    private static ParametresCulture versDomaine(ParametresCultureEntite e) {
        return new ParametresCulture(e.culture, e.rendementReferenceKgParHa, e.humiditeMaxPourcent, e.cycleMinimalJours);
    }
}
