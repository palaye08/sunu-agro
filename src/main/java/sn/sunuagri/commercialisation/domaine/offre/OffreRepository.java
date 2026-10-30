package sn.sunuagri.commercialisation.domaine.offre;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de sortie : stockage des offres. */
public interface OffreRepository {

    Optional<Offre> trouver(UUID id);

    /** Charge l'offre en la verrouillant jusqu'à la fin de la transaction (commandes concurrentes). */
    Optional<Offre> trouverPourModification(UUID id);

    List<Offre> lister(StatutOffre statut);

    Kilogrammes quantiteReservee(TypeCulture culture, Qualite qualite);

    void enregistrer(Offre offre);
}
