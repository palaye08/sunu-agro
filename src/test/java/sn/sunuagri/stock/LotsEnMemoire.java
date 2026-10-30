package sn.sunuagri.stock;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.stock.domaine.Lot;
import sn.sunuagri.stock.domaine.LotRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LotsEnMemoire implements LotRepository {

    private final Map<UUID, Lot> donnees = new LinkedHashMap<>();

    @Override
    public Optional<Lot> trouver(UUID id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public List<Lot> nonEpuises(TypeCulture culture, Qualite qualite) {
        return new ArrayList<>(donnees.values().stream()
                .filter(l -> l.culture() == culture && l.qualite() == qualite && !l.estEpuise()).toList());
    }

    @Override
    public Kilogrammes quantitePresente(TypeCulture culture, Qualite qualite) {
        return donnees.values().stream().filter(l -> l.culture() == culture && l.qualite() == qualite)
                .map(Lot::quantiteRestante).reduce(Kilogrammes.ZERO, Kilogrammes::plus);
    }

    @Override
    public void enregistrer(Lot lot) {
        donnees.put(lot.id(), lot);
    }

    public List<Lot> tous() {
        return List.copyOf(donnees.values());
    }
}
