package sn.sunuagri.partage.infrastructure.web;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Injecte l'utilisateur à l'origine de la requête (lu dans les en-têtes d'identité simulée). */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface UtilisateurConnecte {
}
