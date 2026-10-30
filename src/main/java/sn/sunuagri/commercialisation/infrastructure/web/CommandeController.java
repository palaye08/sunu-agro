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
import sn.sunuagri.commercialisation.application.CommandeService;
import sn.sunuagri.commercialisation.application.LivraisonCommandeService;
import sn.sunuagri.commercialisation.domaine.commande.Commande;
import sn.sunuagri.commercialisation.domaine.commande.StatutCommande;
import sn.sunuagri.paiement.domaine.StatutNotification;
import sn.sunuagri.paiement.domaine.StatutPaiement;
import sn.sunuagri.partage.application.Utilisateur;
import sn.sunuagri.partage.domaine.PageDemandee;
import sn.sunuagri.partage.domaine.PageResultat;
import sn.sunuagri.partage.domaine.Qualite;
import sn.sunuagri.partage.domaine.TypeCulture;
import sn.sunuagri.partage.infrastructure.web.ReponsesErreur;
import sn.sunuagri.partage.infrastructure.web.UtilisateurConnecte;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/commandes")
@Tag(name = "Commandes")
public class CommandeController {

    private final CommandeService service;
    private final LivraisonCommandeService livraisons;

    public CommandeController(CommandeService service, LivraisonCommandeService livraisons) {
        this.service = service;
        this.livraisons = livraisons;
    }

    @PostMapping
    @Operation(summary = "Passer une commande sur une offre ouverte (US-11)",
            description = """
                    Rôle : ACHETEUR. Quantité minimale 50 kg ; la quantité ne dépasse pas le restant de l'offre.
                    Montant = quantité × prix au kg. Statut initial EN_ATTENTE ; l'offre devient EPUISEE
                    quand plus rien ne reste.""")
    @ApiResponse(responseCode = "201", description = "Commande créée")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public ResponseEntity<CommandeReponse> passer(@UtilisateurConnecte Utilisateur utilisateur,
                                                  @Valid @RequestBody PassageCommandeRequete requete) {
        Commande commande = service.passer(utilisateur, requete.offreId(), requete.quantiteKg());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(commande.id()).toUri()).body(CommandeReponse.de(commande));
    }

    @GetMapping
    @Operation(summary = "Consulter l'historique des commandes (US-16)",
            description = "Rôles : ACHETEUR (ses propres commandes uniquement) ou AGENT (toutes). "
                    + "Paginé, de la plus récente à la plus ancienne, filtre de statut facultatif.")
    @ApiResponse(responseCode = "200", description = "Une page de commandes")
    @ApiResponse(responseCode = "400", ref = ReponsesErreur.REQUETE_INVALIDE)
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public PageCommandesReponse historique(@UtilisateurConnecte Utilisateur utilisateur,
                                           @RequestParam(required = false) StatutCommande statut,
                                           @Parameter(description = "Numéro de page, à partir de 0") @RequestParam(defaultValue = "0") int page,
                                           @Parameter(description = "Taille de page (1 à 100)") @RequestParam(defaultValue = "20") int taille) {
        return PageCommandesReponse.de(service.historique(utilisateur, statut, new PageDemandee(page, taille)));
    }

    @GetMapping("/{commandeId}")
    @Operation(summary = "Consulter une commande", description = "Rôles : ACHETEUR (ses commandes) ou AGENT.")
    @ApiResponse(responseCode = "200", description = "La commande")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    public CommandeReponse consulter(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID commandeId) {
        return CommandeReponse.de(service.consulter(utilisateur, commandeId));
    }

    @PostMapping("/{commandeId}/annulation")
    @Operation(summary = "Annuler sa commande (US-12)",
            description = "Rôle : ACHETEUR, propriétaire de la commande. Possible tant que la commande n'est pas livrée ; "
                    + "la quantité annulée redevient disponible dans l'offre.")
    @ApiResponse(responseCode = "200", description = "Commande annulée")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public CommandeReponse annuler(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID commandeId) {
        return CommandeReponse.de(service.annuler(utilisateur, commandeId));
    }

    @PostMapping("/{commandeId}/livraison")
    @Operation(summary = "Confirmer la livraison d'une commande et répartir le paiement (US-13, US-14)",
            description = """
                    Rôle : AGENT. Seule une commande EN_ATTENTE peut être livrée. Les lots de la culture et de la qualité
                    sont consommés du plus ancien au plus récent. La coopérative retient sa commission (5 %), le reste
                    est réparti entre les producteurs au prorata des quantités apportées dans les lots consommés
                    (arrondi au franc par la méthode du plus fort reste : commission + paiements = montant, exactement).
                    Chaque paiement dû est créé « à payer » et un SMS est envoyé au producteur ; un échec d'envoi
                    n'annule pas la livraison et est tracé dans `notificationSms`.""")
    @ApiResponse(responseCode = "200", description = "Commande livrée, paiements dus enregistrés")
    @ApiResponse(responseCode = "403", ref = ReponsesErreur.ACCES_REFUSE)
    @ApiResponse(responseCode = "404", ref = ReponsesErreur.INTROUVABLE)
    @ApiResponse(responseCode = "422", ref = ReponsesErreur.REGLE_METIER)
    public LivraisonCommandeReponse livrer(@UtilisateurConnecte Utilisateur utilisateur, @PathVariable UUID commandeId) {
        return LivraisonCommandeReponse.de(livraisons.livrer(utilisateur, commandeId));
    }

    @Schema(name = "PassageCommande")
    public record PassageCommandeRequete(@NotNull UUID offreId,
                                         @NotNull @Schema(example = "600", description = "Quantité en kg") Long quantiteKg) {
    }

    @Schema(name = "Commande")
    public record CommandeReponse(UUID id, UUID offreId, String acheteurId, TypeCulture culture, Qualite qualite,
                                  long quantiteKg, long prixParKg, long montant, String devise, StatutCommande statut,
                                  Instant dateCreation, Instant dateLivraison) {
        public static CommandeReponse de(Commande c) {
            return new CommandeReponse(c.id(), c.offreId(), c.acheteurId(), c.culture(), c.qualite(),
                    c.quantite().valeur(), c.prixParKg().valeur(), c.montant().valeur(), "XOF", c.statut(),
                    c.dateCreation(), c.dateLivraison());
        }
    }

    @Schema(name = "PageCommandes")
    public record PageCommandesReponse(List<CommandeReponse> elements, int page, int taille, long totalElements,
                                       int totalPages) {
        static PageCommandesReponse de(PageResultat<Commande> p) {
            return new PageCommandesReponse(p.elements().stream().map(CommandeReponse::de).toList(), p.page(),
                    p.taille(), p.totalElements(), p.totalPages());
        }
    }

    @Schema(name = "LivraisonCommande")
    public record LivraisonCommandeReponse(CommandeReponse commande, long montantCommande, long commissionCooperative,
                                           long totalVerseProducteurs, List<LotConsomme> lotsConsommes,
                                           List<PaiementDuResume> paiements) {
        static LivraisonCommandeReponse de(LivraisonCommandeService.LivraisonCommande l) {
            return new LivraisonCommandeReponse(CommandeReponse.de(l.commande()),
                    l.repartition().montantCommande().valeur(), l.repartition().commission().valeur(),
                    l.repartition().totalVerse().valeur(),
                    l.lotsConsommes().stream().map(p -> new LotConsomme(p.lotId(), p.producteurId(), p.quantite().valeur())).toList(),
                    l.paiements().stream().map(p -> new PaiementDuResume(p.id(), p.producteurId(), p.quantite().valeur(),
                            p.montant().valeur(), p.statut(), p.notification())).toList());
        }
    }

    @Schema(name = "LotConsomme")
    public record LotConsomme(UUID lotId, UUID producteurId, long quantiteKg) {
    }

    @Schema(name = "PaiementDuResume")
    public record PaiementDuResume(UUID id, UUID producteurId, long quantiteKg, long montant, StatutPaiement statut,
                                   StatutNotification notificationSms) {
    }
}
