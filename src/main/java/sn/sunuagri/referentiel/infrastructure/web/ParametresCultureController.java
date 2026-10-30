package sn.sunuagri.referentiel.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.partage.infrastructure.web.ReponsesErreur;
import sn.sunuagri.partage.infrastructure.web.UtilisateurConnecte;
import sn.sunuagri.referentiel.application.ParametresCultureService;
import sn.sunuagri.referentiel.domaine.ParametresCulture;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/parametres-cultures")
@Tag(name = "Référentiel")
public class ParametresCultureController {

    private final ParametresCultureService service;

    public ParametresCultureController(ParametresCultureService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lister les paramètres des cultures (US-19)", description = "Rôle : ADMINISTRATEUR.")
    @ApiResponse(responseCode = "200", description = "Paramètres en vigueur")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    public List<ParametresCultureReponse> lister(@UtilisateurConnecte Utilisateur utilisateur) {
        return service.lister(utilisateur).stream().map(ParametresCultureReponse::de).toList();
    }

    @GetMapping("/{culture}")
    @Operation(summary = "Consulter les paramètres d'une culture (US-19)", description = "Rôle : ADMINISTRATEUR.")
    @ApiResponse(responseCode = "200", description = "Paramètres en vigueur")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    public ParametresCultureReponse consulter(@UtilisateurConnecte Utilisateur utilisateur,
                                              @PathVariable TypeCulture culture) {
        return ParametresCultureReponse.de(service.consulter(utilisateur, culture));
    }

    @PutMapping("/{culture}")
    @Operation(summary = "Modifier les paramètres d'une culture (US-19)",
            description = "Rôle : ADMINISTRATEUR. La modification s'applique aux déclarations futures, "
                    + "jamais aux cultures déjà déclarées. Les valeurs doivent être strictement positives.")
    @ApiResponse(responseCode = "200", description = "Paramètres modifiés")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public ParametresCultureReponse modifier(@UtilisateurConnecte Utilisateur utilisateur,
                                             @PathVariable TypeCulture culture,
                                             @Valid @RequestBody ModificationParametresRequete requete) {
        return ParametresCultureReponse.de(service.modifier(utilisateur, culture,
                requete.rendementReferenceKgParHa(), requete.humiditeMaxPourcent(), requete.cycleMinimalJours()));
    }

    @Schema(name = "ModificationParametresCulture")
    public record ModificationParametresRequete(
            @NotNull @Positive @Schema(example = "1200") BigDecimal rendementReferenceKgParHa,
            @NotNull @Positive @Schema(example = "9") BigDecimal humiditeMaxPourcent,
            @NotNull @Positive @Schema(example = "90") Integer cycleMinimalJours) {
    }

    @Schema(name = "ParametresCulture")
    public record ParametresCultureReponse(TypeCulture culture, BigDecimal rendementReferenceKgParHa,
                                           BigDecimal humiditeMaxPourcent, int cycleMinimalJours) {
        static ParametresCultureReponse de(ParametresCulture p) {
            return new ParametresCultureReponse(p.culture(), p.rendementReferenceKgParHa(),
                    p.humiditeMaxPourcent(), p.cycleMinimalJours());
        }
    }
}
