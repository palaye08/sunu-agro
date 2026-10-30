package sn.sunuagri.referentiel.application;

import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Transactions;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.referentiel.domaine.ParametresCulture;
import sn.sunuagri.referentiel.domaine.ParametresCultureRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Cas d'usage US-19 : consulter et modifier les paramètres des cultures. */
public class ParametresCultureService {

    private final ParametresCultureRepository repository;
    private final Transactions transactions;

    public ParametresCultureService(ParametresCultureRepository repository, Transactions transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    public List<ParametresCulture> lister(Utilisateur utilisateur) {
        utilisateur.exigerRole(Role.ADMINISTRATEUR);
        return repository.tous().stream().sorted(Comparator.comparing(ParametresCulture::culture)).toList();
    }

    public ParametresCulture consulter(Utilisateur utilisateur, TypeCulture culture) {
        utilisateur.exigerRole(Role.ADMINISTRATEUR);
        return parametresActuels(culture);
    }

    public ParametresCulture modifier(Utilisateur utilisateur, TypeCulture culture,
                                      BigDecimal rendement, BigDecimal humiditeMax, int cycleMinimal) {
        utilisateur.exigerRole(Role.ADMINISTRATEUR);
        return transactions.executer(() -> {
            ParametresCulture modifies = parametresActuels(culture).modifier(rendement, humiditeMax, cycleMinimal);
            repository.enregistrer(modifies);
            return modifies;
        });
    }

    /** Paramètres en vigueur, utilisés par les autres contextes au moment d'une déclaration. */
    public ParametresCulture parametresActuels(TypeCulture culture) {
        return repository.trouver(culture)
                .orElseThrow(() -> new RessourceIntrouvable("paramètres de culture", culture));
    }
}
