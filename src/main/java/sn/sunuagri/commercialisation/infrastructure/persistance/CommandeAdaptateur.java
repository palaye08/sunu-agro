package sn.sunuagri.commercialisation.infrastructure.persistance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import sn.sunuagri.commercialisation.domaine.commande.Commande;
import sn.sunuagri.commercialisation.domaine.commande.CommandeRepository;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.PageDemandee;
import sn.sunuagri.partage.domaine.PageResultat;

import java.util.Optional;
import java.util.UUID;

@Repository
class CommandeAdaptateur implements CommandeRepository {

    private static final Sort PLUS_RECENTES_D_ABORD = Sort.by(Sort.Order.desc("dateCreation"), Sort.Order.desc("id"));

    private final CommandeJpa jpa;

    CommandeAdaptateur(CommandeJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Commande> trouver(UUID id) {
        return jpa.findById(id).map(CommandeAdaptateur::versDomaine);
    }

    @Override
    public PageResultat<Commande> rechercher(String acheteurId, StatutCommande statut, PageDemandee page) {
        Page<CommandeEntite> resultat = jpa.findAll(CommandeJpa.filtres(acheteurId, statut),
                PageRequest.of(page.page(), page.taille(), PLUS_RECENTES_D_ABORD));
        return new PageResultat<>(resultat.getContent().stream().map(CommandeAdaptateur::versDomaine).toList(),
                page.page(), page.taille(), resultat.getTotalElements());
    }

    @Override
    public void enregistrer(Commande c) {
        CommandeEntite e = jpa.findById(c.id()).orElseGet(CommandeEntite::new);
        e.id = c.id();
        e.offreId = c.offreId();
        e.acheteurId = c.acheteurId();
        e.culture = c.culture();
        e.qualite = c.qualite();
        e.quantiteKg = c.quantite().valeur();
        e.prixParKg = c.prixParKg().valeur();
        e.montant = c.montant().valeur();
        e.dateCreation = c.dateCreation();
        e.statut = c.statut();
        e.dateLivraison = c.dateLivraison();
        jpa.save(e);
    }

    private static Commande versDomaine(CommandeEntite e) {
        return Commande.reconstituer(e.id, e.offreId, e.acheteurId, e.culture, e.qualite, Kilogrammes.de(e.quantiteKg),
                MontantXof.de(e.prixParKg), MontantXof.de(e.montant), e.dateCreation, e.statut, e.dateLivraison);
    }
}
