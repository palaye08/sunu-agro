package sn.sunuagri.paiement.application;

import java.util.Optional;
import java.util.UUID;

/** Port de sortie : numéro de téléphone d'un producteur (fourni par le contexte Exploitation). */
public interface AnnuaireProducteurs {

    Optional<String> telephone(UUID producteurId);
}
