package sn.sunuagri.referentiel.infrastructure.persistance;

import org.springframework.stereotype.Repository;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.DernierPrixConnuRepository;
import sn.sunuagri.referentiel.domaine.PrixReference;

import java.util.Optional;

@Repository
class DernierPrixConnuAdaptateur implements DernierPrixConnuRepository {

    private final DernierPrixConnuJpa jpa;

    DernierPrixConnuAdaptateur(DernierPrixConnuJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PrixReference> trouver(TypeCulture culture) {
        return jpa.findById(culture).map(e -> new PrixReference(e.culture, MontantXof.de(e.prixParKg), e.date));
    }

    @Override
    public void enregistrer(PrixReference prix) {
        DernierPrixConnuEntite e = new DernierPrixConnuEntite();
        e.culture = prix.culture();
        e.prixParKg = prix.prixParKg().valeur();
        e.date = prix.date();
        jpa.save(e);
    }
}
