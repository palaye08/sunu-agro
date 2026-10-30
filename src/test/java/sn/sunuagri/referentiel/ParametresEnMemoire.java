package sn.sunuagri.referentiel;

import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.ParametresCulture;
import sn.sunuagri.referentiel.domaine.ParametresCultureRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adaptateur en mémoire du port, réutilisable par les autres tests. */
public class ParametresEnMemoire implements ParametresCultureRepository {

    private final Map<TypeCulture, ParametresCulture> donnees = new EnumMap<>(TypeCulture.class);

    public static ParametresEnMemoire valeursDuSujet() {
        ParametresEnMemoire repo = new ParametresEnMemoire();
        repo.enregistrer(new ParametresCulture(TypeCulture.ARACHIDE, new BigDecimal("1200"), new BigDecimal("9"), 90));
        repo.enregistrer(new ParametresCulture(TypeCulture.MIL, new BigDecimal("800"), new BigDecimal("12"), 75));
        repo.enregistrer(new ParametresCulture(TypeCulture.RIZ, new BigDecimal("4000"), new BigDecimal("14"), 110));
        repo.enregistrer(new ParametresCulture(TypeCulture.MAIS, new BigDecimal("2500"), new BigDecimal("14"), 90));
        return repo;
    }

    @Override
    public Optional<ParametresCulture> trouver(TypeCulture culture) {
        return Optional.ofNullable(donnees.get(culture));
    }

    @Override
    public List<ParametresCulture> tous() {
        return new ArrayList<>(donnees.values());
    }

    @Override
    public void enregistrer(ParametresCulture parametres) {
        donnees.put(parametres.culture(), parametres);
    }
}
