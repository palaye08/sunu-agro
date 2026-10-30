package sn.sunuagri.commercialisation;

import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.commercialisation.domaine.offre.OffreRepository;
import sn.sunuagri.commercialisation.domaine.offre.StatutOffre;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.stock.domaine.ReservationsPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OffresEnMemoire implements OffreRepository, ReservationsPort {

    private final Map<UUID, Offre> donnees = new LinkedHashMap<>();

    @Override
    public Optional<Offre> trouver(UUID id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public Optional<Offre> trouverPourModification(UUID id) {
        return trouver(id);
    }

    @Override
    public List<Offre> lister(StatutOffre statut) {
        return donnees.values().stream().filter(o -> statut == null || o.statut() == statut).toList();
    }

    @Override
    public Kilogrammes quantiteReservee(TypeCulture culture, Qualite qualite) {
        return donnees.values().stream().filter(o -> o.culture() == culture && o.qualite() == qualite)
                .map(Offre::quantiteReservee).reduce(Kilogrammes.ZERO, Kilogrammes::plus);
    }

    @Override
    public void enregistrer(Offre offre) {
        donnees.put(offre.id(), offre);
    }
}
