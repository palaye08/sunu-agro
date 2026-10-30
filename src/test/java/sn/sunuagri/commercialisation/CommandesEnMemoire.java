package sn.sunuagri.commercialisation;

import sn.sunuagri.commercialisation.domaine.commande.Commande;
import sn.sunuagri.commercialisation.domaine.commande.CommandeRepository;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;
import sn.sunuagri.partage.domaine.PageDemandee;
import sn.sunuagri.partage.domaine.PageResultat;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CommandesEnMemoire implements CommandeRepository {

    private final Map<UUID, Commande> donnees = new LinkedHashMap<>();

    @Override
    public Optional<Commande> trouver(UUID id) {
        return Optional.ofNullable(donnees.get(id));
    }

    @Override
    public PageResultat<Commande> rechercher(String acheteurId, StatutCommande statut, PageDemandee page) {
        List<Commande> filtrees = donnees.values().stream()
                .filter(c -> acheteurId == null || Objects.equals(c.acheteurId(), acheteurId))
                .filter(c -> statut == null || c.statut() == statut)
                .sorted(Comparator.comparing(Commande::dateCreation).reversed())
                .toList();
        List<Commande> contenu = filtrees.stream().skip((long) page.page() * page.taille()).limit(page.taille()).toList();
        return new PageResultat<>(contenu, page.page(), page.taille(), filtrees.size());
    }

    @Override
    public void enregistrer(Commande commande) {
        donnees.put(commande.id(), commande);
    }
}
