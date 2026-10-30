package sn.sunuagri;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Vérifie automatiquement les règles de l'architecture hexagonale (ADR-001) :
 * les dépendances vont de l'infrastructure vers l'application puis vers le domaine, jamais l'inverse.
 */
class ArchitectureHexagonaleTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("sn.sunuagri");

    @Test
    void le_domaine_ne_depend_d_aucun_framework() {
        noClasses().that().resideInAPackage("..domaine..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "org.hibernate..", "jakarta.validation..",
                        "com.fasterxml..", "io.swagger..", "org.slf4j..")
                .check(CLASSES);
    }

    @Test
    void le_domaine_ne_depend_ni_de_l_application_ni_de_l_infrastructure() {
        noClasses().that().resideInAPackage("..domaine..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
                .check(CLASSES);
    }

    @Test
    void l_application_ne_depend_ni_de_spring_ni_de_l_infrastructure() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "..infrastructure..")
                .check(CLASSES);
    }
}
