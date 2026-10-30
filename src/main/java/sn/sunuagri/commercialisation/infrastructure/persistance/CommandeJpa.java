package sn.sunuagri.commercialisation.infrastructure.persistance;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;

import java.util.UUID;

interface CommandeJpa extends JpaRepository<CommandeEntite, UUID>, JpaSpecificationExecutor<CommandeEntite> {

    static Specification<CommandeEntite> filtres(String acheteurId, StatutCommande statut) {
        Specification<CommandeEntite> spec = (racine, requete, cb) -> cb.conjunction();
        if (acheteurId != null) {
            spec = spec.and((racine, requete, cb) -> cb.equal(racine.get("acheteurId"), acheteurId));
        }
        if (statut != null) {
            spec = spec.and((racine, requete, cb) -> cb.equal(racine.get("statut"), statut));
        }
        return spec;
    }
}
