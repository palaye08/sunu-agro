package sn.sunuagri.commercialisation.infrastructure.persistance;

import org.springframework.stereotype.Repository;
import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.commercialisation.domaine.offre.OffreRepository;
import sn.sunuagri.commercialisation.domaine.offre.StatutOffre;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.stock.domaine.ReservationsPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptateur JPA des offres ; implémente aussi le port {@link ReservationsPort} du Stock. */
@Repository
class OffreAdaptateur implements OffreRepository, ReservationsPort {

    private final OffreJpa jpa;

    OffreAdaptateur(OffreJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Offre> trouver(UUID id) {
        return jpa.findById(id).map(OffreAdaptateur::versDomaine);
    }

    @Override
    public Optional<Offre> trouverPourModification(UUID id) {
        return jpa.verrouiller(id).map(OffreAdaptateur::versDomaine);
    }

    @Override
    public List<Offre> lister(StatutOffre statut) {
        return (statut == null ? jpa.findAll() : jpa.findByStatut(statut)).stream().map(OffreAdaptateur::versDomaine).toList();
    }

    @Override
    public Kilogrammes quantiteReservee(TypeCulture culture, Qualite qualite) {
        return Kilogrammes.de(jpa.sommeReservee(culture, qualite));
    }

    @Override
    public void enregistrer(Offre o) {
        OffreEntite e = jpa.findById(o.id()).orElseGet(OffreEntite::new);
        e.id = o.id();
        e.culture = o.culture();
        e.qualite = o.qualite();
        e.quantiteKg = o.quantite().valeur();
        e.prixParKg = o.prixParKg().valeur();
        e.datePublication = o.datePublication();
        e.avertissementPrix = o.avertissementPrix();
        e.statut = o.statut();
        e.quantiteCommandeeKg = o.quantiteCommandee().valeur();
        e.quantiteLivreeKg = o.quantiteLivree().valeur();
        e.quantiteReserveeKg = o.quantiteReservee().valeur();
        jpa.save(e);
    }

    private static Offre versDomaine(OffreEntite e) {
        return Offre.reconstituer(e.id, e.culture, e.qualite, Kilogrammes.de(e.quantiteKg), MontantXof.de(e.prixParKg),
                e.datePublication, e.avertissementPrix, e.statut, Kilogrammes.de(e.quantiteCommandeeKg),
                Kilogrammes.de(e.quantiteLivreeKg));
    }
}
