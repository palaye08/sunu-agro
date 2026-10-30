package sn.sunuagri.referentiel.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.sunuagri.partage.domaine.TypeCulture;

interface ParametresCultureJpa extends JpaRepository<ParametresCultureEntite, TypeCulture> {
}
