package sn.sunuagri.paiement.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PaiementDuJpa extends JpaRepository<PaiementDuEntite, UUID> {

    List<PaiementDuEntite> findByProducteurId(UUID producteurId);
}
