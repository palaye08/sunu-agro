package sn.sunuagri.commercialisation.infrastructure.persistance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import sn.sunuagri.commercialisation.domaine.offre.StatutOffre;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OffreJpa extends JpaRepository<OffreEntite, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OffreEntite o where o.id = :id")
    Optional<OffreEntite> verrouiller(UUID id);

    List<OffreEntite> findByStatut(StatutOffre statut);

    @Query("select coalesce(sum(o.quantiteReserveeKg), 0) from OffreEntite o where o.culture = :culture and o.qualite = :qualite")
    long sommeReservee(TypeCulture culture, Qualite qualite);
}
