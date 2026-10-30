package sn.sunuagri.partage.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import sn.sunuagri.partage.application.AccesRefuse;
import sn.sunuagri.partage.application.NonAuthentifie;
import sn.sunuagri.partage.application.ServiceExterneIndisponible;
import sn.sunuagri.partage.domaine.ExceptionMetier;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;
import sn.sunuagri.partage.domaine.RessourceDejaExistante;
import sn.sunuagri.partage.domaine.RessourceIntrouvable;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Traduit toute exception en {@link ErreurApi} (format unique, E6). */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger LOG = LoggerFactory.getLogger(GestionnaireErreurs.class);
    public static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @ExceptionHandler(ExceptionMetier.class)
    ResponseEntity<ErreurApi> metier(ExceptionMetier e, HttpServletRequest requete) {
        HttpStatus statut = switch (e) {
            case RessourceIntrouvable ignored -> HttpStatus.NOT_FOUND;
            case RessourceDejaExistante ignored -> HttpStatus.CONFLICT;
            case AccesRefuse ignored -> HttpStatus.FORBIDDEN;
            case NonAuthentifie ignored -> HttpStatus.UNAUTHORIZED;
            case ServiceExterneIndisponible ignored -> HttpStatus.SERVICE_UNAVAILABLE;
            case RegleMetierEnfreinte ignored -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return reponse(statut, e.code(), e.getMessage(), requete, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErreurApi> validation(MethodArgumentNotValidException e, HttpServletRequest requete) {
        List<ErreurApi.Violation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErreurApi.Violation(f.getField(), f.getDefaultMessage()))
                .toList();
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE",
                "La requête contient des champs invalides.", requete, violations);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErreurApi> validationParametres(HandlerMethodValidationException e, HttpServletRequest requete) {
        List<ErreurApi.Violation> violations = e.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(err -> new ErreurApi.Violation(r.getMethodParameter().getParameterName(), err.getDefaultMessage())))
                .toList();
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE",
                "La requête contient des paramètres invalides.", requete, violations);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ResponseEntity<ErreurApi> requeteMalFormee(Exception e, HttpServletRequest requete) {
        String detail = switch (e) {
            case MethodArgumentTypeMismatchException m -> "Valeur invalide pour le paramètre « " + m.getName() + " ».";
            case MissingServletRequestParameterException m -> "Paramètre obligatoire manquant : « " + m.getParameterName() + " ».";
            case MissingServletRequestPartException m -> "Fichier obligatoire manquant : « " + m.getRequestPartName() + " ».";
            default -> "Le corps de la requête est illisible ou contient une valeur invalide.";
        };
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE", detail, requete, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErreurApi> routeInconnue(NoResourceFoundException e, HttpServletRequest requete) {
        return reponse(HttpStatus.NOT_FOUND, "RESSOURCE_INTROUVABLE", "Aucune ressource à cette adresse.", requete, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErreurApi> methode(HttpRequestMethodNotSupportedException e, HttpServletRequest requete) {
        return reponse(HttpStatus.METHOD_NOT_ALLOWED, "METHODE_NON_AUTORISEE",
                "La méthode " + e.getMethod() + " n'est pas supportée sur cette ressource.", requete, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErreurApi> media(HttpMediaTypeNotSupportedException e, HttpServletRequest requete) {
        return reponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TYPE_CONTENU_NON_SUPPORTE",
                "Type de contenu non supporté.", requete, List.of());
    }

    /** Filet de sécurité : une contrainte d'unicité en base attrapée en concurrence. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErreurApi> integrite(DataIntegrityViolationException e, HttpServletRequest requete) {
        LOG.warn("Violation d'intégrité sur {}", requete.getRequestURI(), e);
        return reponse(HttpStatus.CONFLICT, "CONFLIT",
                "La demande entre en conflit avec une donnée existante.", requete, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErreurApi> inattendue(Exception e, HttpServletRequest requete) {
        LOG.error("Erreur inattendue sur {}", requete.getRequestURI(), e);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur inattendue est survenue. Veuillez réessayer plus tard.", requete, List.of());
    }

    private ResponseEntity<ErreurApi> reponse(HttpStatus statut, String code, String detail,
                                              HttpServletRequest requete, List<ErreurApi.Violation> violations) {
        ErreurApi erreur = new ErreurApi(
                "https://api.sunuagri.sn/erreurs/" + code.toLowerCase(Locale.ROOT).replace('_', '-'),
                titre(statut), statut.value(), detail, requete.getRequestURI(), code, Instant.now(), violations);
        return ResponseEntity.status(statut).contentType(PROBLEM_JSON).body(erreur);
    }

    private static String titre(HttpStatus statut) {
        return switch (statut) {
            case BAD_REQUEST -> "Requête invalide";
            case UNAUTHORIZED -> "Non authentifié";
            case FORBIDDEN -> "Accès refusé";
            case NOT_FOUND -> "Ressource introuvable";
            case CONFLICT -> "Conflit";
            case UNPROCESSABLE_ENTITY -> "Règle métier non respectée";
            case METHOD_NOT_ALLOWED -> "Méthode non autorisée";
            case SERVICE_UNAVAILABLE -> "Service indisponible";
            case UNSUPPORTED_MEDIA_TYPE -> "Type de contenu non supporté";
            default -> "Erreur interne";
        };
    }
}
