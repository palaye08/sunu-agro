package sn.sunuagri.referentiel.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.partage.infrastructure.web.ReponsesErreur;
import sn.sunuagri.partage.infrastructure.web.UtilisateurConnecte;
import sn.sunuagri.referentiel.application.PrixMarcheService;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/prix-marche")
@Tag(name = "Référentiel")
public class PrixMarcheController {

    private final PrixMarcheService service;

    public PrixMarcheController(PrixMarcheService service) {
        this.service = service;
    }

    @GetMapping("/{culture}")
    @Operation(summary = "Consulter le prix de référence du marché d'une culture (US-18)",
            description = "Rôle : AGENT. Prix fourni par un service externe ; s'il est indisponible, le dernier prix "
                    + "connu est rendu avec sa date (source = DERNIER_PRIX_CONNU).")
    @ApiResponse(responseCode = "200", description = "Prix de référence")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "503", ref = ReponsesErreur.SERVICE_INDISPONIBLE)
    public PrixMarcheReponse consulter(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable TypeCulture culture) {
        PrixMarcheService.PrixMarche p = service.consulter(utilisateur, culture);
        return new PrixMarcheReponse(culture, p.prix().prixParKg().valeur(), "XOF", p.prix().date(), p.source());
    }

    @Schema(name = "PrixMarche")
    public record PrixMarcheReponse(TypeCulture culture, long prixParKg, String devise,
                                    @Schema(description = "Date du prix") Instant date,
                                    PrixMarcheService.Source source) {
    }
}
