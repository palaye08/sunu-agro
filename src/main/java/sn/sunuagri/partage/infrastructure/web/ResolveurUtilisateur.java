package sn.sunuagri.partage.infrastructure.web;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import sn.sunuagri.partage.application.NonAuthentifie;
import sn.sunuagri.partage.application.Role;
import sn.sunuagri.partage.application.Utilisateur;

import java.util.Locale;

/**
 * Adaptateur d'entrée : identité SIMULÉE (hors périmètre du sujet).
 * L'utilisateur est lu dans les en-têtes {@value #EN_TETE_ROLE} et {@value #EN_TETE_IDENTIFIANT}.
 * En production, ce composant serait remplacé par la validation d'un jeton (OIDC/JWT)
 * sans toucher aux règles d'accès, qui sont dans la couche application.
 */
@Component
public class ResolveurUtilisateur implements HandlerMethodArgumentResolver {

    public static final String EN_TETE_ROLE = "X-Role";
    public static final String EN_TETE_IDENTIFIANT = "X-Utilisateur-Id";

    @Override
    public boolean supportsParameter(MethodParameter parametre) {
        return parametre.hasParameterAnnotation(UtilisateurConnecte.class)
                && parametre.getParameterType().equals(Utilisateur.class);
    }

    @Override
    public Utilisateur resolveArgument(MethodParameter parametre, ModelAndViewContainer mav,
                                       NativeWebRequest requete, WebDataBinderFactory binder) {
        String role = requete.getHeader(EN_TETE_ROLE);
        if (role == null || role.isBlank()) {
            throw new NonAuthentifie("En-tête " + EN_TETE_ROLE + " manquant (AGENT, PRODUCTEUR, ACHETEUR ou ADMINISTRATEUR).");
        }
        Role roleLu;
        try {
            roleLu = Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NonAuthentifie("Rôle inconnu : " + role + ".");
        }
        String identifiant = requete.getHeader(EN_TETE_IDENTIFIANT);
        if ((roleLu == Role.PRODUCTEUR || roleLu == Role.ACHETEUR) && (identifiant == null || identifiant.isBlank())) {
            throw new NonAuthentifie("En-tête " + EN_TETE_IDENTIFIANT + " obligatoire pour le rôle " + roleLu + ".");
        }
        return new Utilisateur(roleLu, identifiant == null ? roleLu.name().toLowerCase(Locale.ROOT) : identifiant.trim());
    }
}
