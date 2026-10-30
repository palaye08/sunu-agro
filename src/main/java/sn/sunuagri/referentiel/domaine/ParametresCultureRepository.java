package sn.sunuagri.referentiel.domaine;

import sn.sunuagri.partage.domaine.TypeCulture;

import java.util.List;
import java.util.Optional;

/** Port de sortie : stockage des paramètres des cultures. */
public interface ParametresCultureRepository {

    Optional<ParametresCulture> trouver(TypeCulture culture);

    List<ParametresCulture> tous();

    void enregistrer(ParametresCulture parametres);
}
