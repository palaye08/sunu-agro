package sn.sunuagri.stock.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de sortie : stockage des lots. */
public interface LotRepository {

    Optional<Lot> trouver(UUID id);

    /** Lots non épuisés d'une culture et d'une qualité. */
    List<Lot> nonEpuises(TypeCulture culture, Qualite qualite);

    Kilogrammes quantitePresente(TypeCulture culture, Qualite qualite);

    void enregistrer(Lot lot);
}
