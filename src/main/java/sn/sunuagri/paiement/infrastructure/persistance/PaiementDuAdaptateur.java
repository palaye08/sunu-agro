package sn.sunuagri.paiement.infrastructure.persistance;

import org.springframework.stereotype.Repository;
import sn.sunuagri.paiement.domaine.PaiementDu;
import sn.sunuagri.paiement.domaine.PaiementDuRepository;
import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PaiementDuAdaptateur implements PaiementDuRepository {

    private final PaiementDuJpa jpa;

    PaiementDuAdaptateur(PaiementDuJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PaiementDu> trouver(UUID id) {
        return jpa.findById(id).map(PaiementDuAdaptateur::versDomaine);
    }

    @Override
    public List<PaiementDu> duProducteur(UUID producteurId) {
        return jpa.findByProducteurId(producteurId).stream().map(PaiementDuAdaptateur::versDomaine).toList();
    }

    @Override
    public void enregistrer(PaiementDu p) {
        PaiementDuEntite e = jpa.findById(p.id()).orElseGet(PaiementDuEntite::new);
        e.id = p.id();
        e.commandeId = p.commandeId();
        e.producteurId = p.producteurId();
        e.culture = p.culture();
        e.quantiteKg = p.quantite().valeur();
        e.montant = p.montant().valeur();
        e.statut = p.statut();
        e.dateCreation = p.dateCreation();
        e.notification = p.notification();
        e.dateNotification = p.dateNotification();
        e.motifEchecNotification = p.motifEchecNotification();
        jpa.save(e);
    }

    private static PaiementDu versDomaine(PaiementDuEntite e) {
        return PaiementDu.reconstituer(e.id, e.commandeId, e.producteurId, e.culture, Kilogrammes.de(e.quantiteKg),
                MontantXof.de(e.montant), e.statut, e.dateCreation, e.notification, e.dateNotification,
                e.motifEchecNotification);
    }
}
