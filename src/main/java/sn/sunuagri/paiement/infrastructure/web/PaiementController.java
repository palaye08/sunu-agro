package sn.sunuagri.paiement.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.sunuagri.paiement.application.PaiementService;
import sn.sunuagri.paiement.domaine.PaiementDu;
import sn.sunuagri.paiement.domaine.StatutNotification;
import sn.sunuagri.paiement.domaine.StatutPaiement;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.partage.infrastructure.web.ReponsesErreur;
import sn.sunuagri.partage.infrastructure.web.UtilisateurConnecte;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Paiements")
public class PaiementController {

    private final PaiementService service;

    public PaiementController(PaiementService service) {
        this.service = service;
    }

    @GetMapping("/producteurs/{producteurId}/paiements")
    @Operation(summary = "Lister les paiements dus à un producteur",
            description = "Rôles : AGENT, ou PRODUCTEUR pour ses propres paiements uniquement.")
    @ApiResponse(responseCode = "200", description = "Paiements dus, du plus récent au plus ancien")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    public List<PaiementDuReponse> duProducteur(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID producteurId) {
        return service.duProducteur(utilisateur, producteurId).stream().map(PaiementDuReponse::de).toList();
    }

    @GetMapping("/paiements/{paiementId}")
    @Operation(summary = "Consulter un paiement dû et le suivi de son SMS",
            description = "Rôles : AGENT, ou PRODUCTEUR pour ses propres paiements uniquement.")
    @ApiResponse(responseCode = "200", description = "Le paiement dû")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    public PaiementDuReponse consulter(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID paiementId) {
        return PaiementDuReponse.de(service.consulter(utilisateur, paiementId));
    }

    @Schema(name = "PaiementDu")
    public record PaiementDuReponse(UUID id, UUID commandeId, UUID producteurId, TypeCulture culture, long quantiteKg,
                                    long montant, String devise, StatutPaiement statut, Instant dateCreation,
                                    NotificationSms notificationSms) {
        static PaiementDuReponse de(PaiementDu p) {
            return new PaiementDuReponse(p.id(), p.commandeId(), p.producteurId(), p.culture(), p.quantite().valeur(),
                    p.montant().valeur(), "XOF", p.statut(), p.dateCreation(),
                    new NotificationSms(p.notification(), p.dateNotification(), p.motifEchecNotification()));
        }
    }

    @Schema(name = "NotificationSms")
    public record NotificationSms(StatutNotification statut, Instant date, String motifEchec) {
    }
}
