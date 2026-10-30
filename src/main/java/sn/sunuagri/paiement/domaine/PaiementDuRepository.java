package sn.sunuagri.paiement.domaine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de sortie : stockage des paiements dus. */
public interface PaiementDuRepository {

    Optional<PaiementDu> trouver(UUID id);

    List<PaiementDu> duProducteur(UUID producteurId);

    void enregistrer(PaiementDu paiement);
}
