package sn.sunuagri.partage.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Format UNIQUE de toutes les erreurs de l'API (E6), compatible RFC 9457 (Problem Details),
 * servi avec le type de contenu {@code application/problem+json}.
 * Aucun détail interne (pile d'appels, SQL, nom de classe) n'y figure.
 */
@Schema(name = "Erreur", description = "Format unique des erreurs (RFC 9457 - Problem Details)")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErreurApi(
        @Schema(description = "URI identifiant le type d'erreur", example = "https://api.sunuagri.sn/erreurs/telephone-deja-utilise")
        String type,
        @Schema(description = "Intitulé court du statut", example = "Conflit")
        String title,
        @Schema(description = "Code HTTP", example = "409")
        int status,
        @Schema(description = "Explication lisible destinée à l'utilisateur",
                example = "Le numéro de téléphone +221771234567 est déjà utilisé par un producteur de la coopérative.")
        String detail,
        @Schema(description = "Chemin de la requête", example = "/api/v1/producteurs")
        String instance,
        @Schema(description = "Code d'erreur stable, exploitable par les clients", example = "TELEPHONE_DEJA_UTILISE")
        String code,
        @Schema(description = "Horodatage de l'erreur (UTC)")
        Instant horodatage,
        @Schema(description = "Détail des champs invalides (erreurs de validation uniquement)")
        List<Violation> violations
) {

    @Schema(name = "Violation", description = "Champ invalide")
    public record Violation(
            @Schema(example = "telephone") String champ,
            @Schema(example = "ne doit pas être vide") String message) {
    }
}
