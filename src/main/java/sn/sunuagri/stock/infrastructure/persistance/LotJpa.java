package sn.sunuagri.stock.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;

import java.util.List;
import java.util.UUID;

interface LotJpa extends JpaRepository<LotEntite, UUID> {

    List<LotEntite> findByCultureAndQualiteAndQuantiteRestanteKgGreaterThan(TypeCulture culture, Qualite qualite, long seuil);

    @Query("select coalesce(sum(l.quantiteRestanteKg), 0) from LotEntite l where l.culture = :culture and l.qualite = :qualite")
    long sommeRestante(TypeCulture culture, Qualite qualite);
}
