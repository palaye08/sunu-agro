package sn.sunuagri.stock.infrastructure.web;

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
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.partage.infrastructure.web.ReponsesErreur;
import sn.sunuagri.partage.infrastructure.web.UtilisateurConnecte;
import sn.sunuagri.stock.application.LivraisonEntrepotService;
import sn.sunuagri.stock.application.StockService;
import sn.sunuagri.stock.domaine.EtatStock;
import sn.sunuagri.stock.domaine.Lot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Stock")
public class StockController {

    private final LivraisonEntrepotService livraisons;
    private final StockService stock;

    public StockController(LivraisonEntrepotService livraisons, StockService stock) {
        this.livraisons = livraisons;
        this.stock = stock;
    }

    @PostMapping("/cultures/{cultureId}/livraisons")
    @Operation(summary = "Livrer une récolte à l'entrepôt (US-08)",
            description = """
                    Rôle : AGENT. La culture doit être récoltée ; le cumul livré ne dépasse pas la quantité récoltée ;
                    l'humidité ne dépasse pas le seuil de la culture. Une livraison acceptée crée un lot identifiable,
                    rattaché au producteur et daté. Une livraison refusée ne modifie rien.""")
    @ApiResponse(responseCode = "201", description = "Livraison acceptée : lot créé")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public ResponseEntity<LotReponse> livrer(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID cultureId,
                                             @Valid @RequestBody LivraisonEntrepotRequete requete) {
        Lot lot = livraisons.livrer(utilisateur, cultureId, new LivraisonEntrepotService.LivraisonEntrepot(
                requete.quantiteKg(), requete.tauxHumidite(), requete.qualite()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/lots/{id}").buildAndExpand(lot.id()).toUri()).body(LotReponse.de(lot));
    }

    @GetMapping("/lots/{lotId}")
    @Operation(summary = "Consulter un lot de stock", description = "Rôles : AGENT, ou PRODUCTEUR pour ses propres lots.")
    @ApiResponse(responseCode = "200", description = "Le lot")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    public LotReponse lot(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID lotId) {
        return LotReponse.de(livraisons.consulter(utilisateur, lotId));
    }

    @GetMapping("/stocks")
    @Operation(summary = "Consulter le stock par culture et par qualité (US-09)",
            description = "Rôle : AGENT. Disponible = total − réservé (quantités engagées dans les offres).")
    @ApiResponse(responseCode = "200", description = "État du stock")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    public List<EtatStockReponse> consulter(@UtilisateurConnecte Utilisateur utilisateur,
                                            @Parameter(description = "Filtre facultatif") @RequestParam(required = false) TypeCulture culture,
                                            @Parameter(description = "Filtre facultatif") @RequestParam(required = false) Qualite qualite) {
        return stock.consulter(utilisateur, culture, qualite).stream().map(EtatStockReponse::de).toList();
    }

    @Schema(name = "LivraisonEntrepot")
    public record LivraisonEntrepotRequete(
            @NotNull @Schema(example = "1500", description = "Quantité livrée en kg") Long quantiteKg,
            @NotNull @Schema(example = "8.5", description = "Taux d'humidité en %") BigDecimal tauxHumidite,
            @NotNull @Schema(example = "A") Qualite qualite) {
    }

    @Schema(name = "Lot")
    public record LotReponse(UUID id, UUID producteurId, UUID cultureId, TypeCulture culture, Qualite qualite,
                             long quantiteKg, long quantiteRestanteKg, BigDecimal tauxHumidite, Instant dateReception) {
        static LotReponse de(Lot l) {
            return new LotReponse(l.id(), l.producteurId(), l.cultureId(), l.culture(), l.qualite(),
                    l.quantiteInitiale().valeur(), l.quantiteRestante().valeur(), l.tauxHumidite(), l.dateReception());
        }
    }

    @Schema(name = "EtatStock")
    public record EtatStockReponse(TypeCulture culture, Qualite qualite, long totalKg, long reserveKg, long disponibleKg) {
        static EtatStockReponse de(EtatStock e) {
            return new EtatStockReponse(e.culture(), e.qualite(), e.total().valeur(), e.reserve().valeur(),
                    e.disponible().valeur());
        }
    }
}
