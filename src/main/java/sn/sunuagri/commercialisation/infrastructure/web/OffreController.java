package sn.sunuagri.commercialisation.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sn.sunuagri.commercialisation.application.OffreService;
import sn.sunuagri.commercialisation.domaine.offre.Offre;
import sn.sunuagri.commercialisation.domaine.offre.StatutOffre;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.partage.infrastructure.web.ReponsesErreur;
import sn.sunuagri.partage.infrastructure.web.UtilisateurConnecte;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offres")
@Tag(name = "Offres")
public class OffreController {

    private final OffreService service;

    public OffreController(OffreService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Publier une offre de vente (US-10, US-18)",
            description = """
                    Rôle : AGENT. La quantité ne dépasse pas le stock disponible de la culture et de la qualité ;
                    le prix au kg (XOF) est strictement positif. L'offre publiée réserve sa quantité dans le stock.
                    Si le prix s'écarte de plus de 30 % du prix de référence du marché, l'offre est publiée avec
                    un avertissement (champ `avertissementPrix`), sans être refusée.""")
    @ApiResponse(responseCode = "201", description = "Offre publiée (statut OUVERTE)")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public ResponseEntity<OffreReponse> publier(@UtilisateurConnecte Utilisateur utilisateur,
                                                @Valid @RequestBody PublicationOffreRequete requete) {
        Offre offre = service.publier(utilisateur, new OffreService.PublicationOffre(requete.culture(), requete.qualite(),
                requete.quantiteKg(), requete.prixParKg()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(offre.id()).toUri()).body(OffreReponse.de(offre));
    }

    @GetMapping
    @Operation(summary = "Lister les offres",
            description = "Rôles : ACHETEUR (offres ouvertes uniquement) ou AGENT (toutes, filtre de statut facultatif).")
    @ApiResponse(responseCode = "200", description = "Offres, de la plus récente à la plus ancienne")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    public List<OffreReponse> lister(@UtilisateurConnecte Utilisateur utilisateur,
                                     @Parameter(description = "Filtre facultatif (ignoré pour un acheteur)")
                                     @RequestParam(required = false) StatutOffre statut) {
        return service.lister(utilisateur, statut).stream().map(OffreReponse::de).toList();
    }

    @GetMapping("/{offreId}")
    @Operation(summary = "Consulter une offre",
            description = "Rôles : ACHETEUR (offre ouverte uniquement) ou AGENT.")
    @ApiResponse(responseCode = "200", description = "L'offre")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    public OffreReponse consulter(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID offreId) {
        return OffreReponse.de(service.consulter(utilisateur, offreId));
    }

    @PostMapping("/{offreId}/retrait")
    @Operation(summary = "Retirer une offre (US-10)",
            description = "Rôle : AGENT. La quantité non vendue est libérée ; les commandes en attente restent honorées.")
    @ApiResponse(responseCode = "200", description = "Offre retirée")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public OffreReponse retirer(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID offreId) {
        return OffreReponse.de(service.retirer(utilisateur, offreId));
    }

    @Schema(name = "PublicationOffre")
    public record PublicationOffreRequete(
            @NotNull @Schema(example = "ARACHIDE") TypeCulture culture,
            @NotNull @Schema(example = "A") Qualite qualite,
            @NotNull @Schema(example = "1000", description = "Quantité en kg") Long quantiteKg,
            @NotNull @Schema(example = "350", description = "Prix au kg en XOF") Long prixParKg) {
    }

    @Schema(name = "Offre")
    public record OffreReponse(UUID id, TypeCulture culture, Qualite qualite, long quantiteKg, long quantiteRestanteKg,
                               long prixParKg, String devise, StatutOffre statut, Instant datePublication,
                               @Schema(description = "Présent si le prix s'écarte de plus de 30 % du prix de référence (US-18)")
                               String avertissementPrix) {
        static OffreReponse de(Offre o) {
            return new OffreReponse(o.id(), o.culture(), o.qualite(), o.quantite().valeur(),
                    o.quantiteRestante().valeur(), o.prixParKg().valeur(), "XOF", o.statut(), o.datePublication(),
                    o.avertissementPrix());
        }
    }
}
