package sn.sunuagri.referentiel.domaine;

import sn.sunuagri.partage.domaine.TypeCulture;

import java.util.Optional;

/** Port de sortie : mémorise le dernier prix obtenu du service externe, pour le cas où il serait indisponible. */
public interface DernierPrixConnuRepository {

    Optional<PrixReference> trouver(TypeCulture culture);

    void enregistrer(PrixReference prix);
}
