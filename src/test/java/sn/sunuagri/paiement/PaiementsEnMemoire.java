package sn.sunuagri.paiement;

import sn.sunuagri.paiement.domaine.PaiementDu;
import sn.sunuagri.paiement.domaine.PaiementDuRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PaiementsEnMemoire implements PaiementDuRepository {

    private final Map<UUID, PaiementDu> donnees = new LinkedHashMap<>();

    @Override
    public Optional<PaiementDu> trouver(UUID id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<PaiementDu> duProducteur(UUID producteurId) {
        return donnees.values().stream().filter(p -> p.producteurId().equals(producteurId)).toList();
    }

    @Override
    public void enregistrer(PaiementDu paiement) {
        donnees.put(paiement.id(), paiement);
    }

    public List<PaiementDu> tous() {
        return List.copyOf(donnees.values());
    }
}
