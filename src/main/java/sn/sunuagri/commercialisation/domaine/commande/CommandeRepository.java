package sn.sunuagri.commercialisation.domaine.commande;

import sn.sunuagri.partage.domaine.PageDemandee;
import sn.sunuagri.partage.domaine.PageResultat;

import java.util.Optional;
import java.util.UUID;

/** Port de sortie : stockage des commandes. */
public interface CommandeRepository {

    Optional<Commande> trouver(UUID id);

    /**
     * Commandes de la plus récente à la plus ancienne, paginées côté base.
     * {@code acheteurId} et {@code statut} sont des filtres facultatifs (null = pas de filtre).
     */
    PageResultat<Commande> rechercher(String acheteurId, StatutCommande statut, PageDemandee page);

    void enregistrer(Commande commande);
}
