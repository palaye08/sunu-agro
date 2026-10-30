package sn.sunuagri.partage.infrastructure.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.sunuagri.partage.infrastructure.web.ErreurApi;
import sn.sunuagri.partage.infrastructure.web.ResolveurUtilisateur;

import java.util.List;

/** Contrat OpenAPI (E5) : informations générales, identité simulée, réponses d'erreur communes (E6). */
@Configuration
public class ConfigurationOpenApi {

    private static final String SCHEMA_ERREUR = "#/components/schemas/Erreur";

    @Bean
    OpenAPI contratSunuAgri() {
        Components composants = new Components()
                .addSecuritySchemes("role", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER).name(ResolveurUtilisateur.EN_TETE_ROLE)
                        .description("Identité simulée : AGENT, PRODUCTEUR, ACHETEUR ou ADMINISTRATEUR"))
                .addSecuritySchemes("utilisateur", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER).name(ResolveurUtilisateur.EN_TETE_IDENTIFIANT)
                        .description("Identifiant : id du producteur (rôle PRODUCTEUR) ou de l'acheteur (rôle ACHETEUR)"));

        ModelConverters.getInstance().readAll(ErreurApi.class).forEach(composants::addSchemas);
        composants.addResponses("RequeteInvalide", erreur("Requête mal formée ou champs invalides"));
        composants.addResponses("NonAuthentifie", erreur("Identité absente ou invalide (en-têtes X-Role / X-Utilisateur-Id)"));
        composants.addResponses("AccesRefuse", erreur("Le rôle de l'utilisateur ne permet pas cette opération"));
        composants.addResponses("Introuvable", erreur("Ressource introuvable (ou appartenant à un autre utilisateur)"));
        composants.addResponses("Conflit", erreur("Une ressource identique existe déjà"));
        composants.addResponses("RegleMetier", erreur("Une règle métier n'est pas respectée"));
        composants.addResponses("ServiceIndisponible", erreur("Service externe indisponible et aucune valeur de repli"));
        composants.addResponses("ErreurInterne", erreur("Erreur inattendue, sans détail interne"));

        return new OpenAPI()
                .info(new Info()
                        .title("SunuAgri API")
                        .version("v1")
                        .description("""
                                API de la coopérative agricole SunuAgri : producteurs, parcelles, campagnes, cultures,
                                récoltes, stock, offres, commandes et paiements dus.

                                **Identité simulée** : renseignez les en-têtes `X-Role` et `X-Utilisateur-Id`
                                via le bouton *Authorize*.

                                **Erreurs** : toutes les erreurs suivent le format unique `Erreur`
                                (RFC 9457, `application/problem+json`) avec un `code` stable.

                                **Évolution** : l'API est versionnée dans le chemin (`/api/v1`) ; les évolutions
                                compatibles (ajout de champs, de ressources) se font sans changer de version.
                                """)
                        .contact(new Contact().name("Coopérative SunuAgri")))
                .components(composants)
                .security(List.of(new SecurityRequirement().addList("role").addList("utilisateur")))
                .tags(List.of(
                        new Tag().name("Producteurs").description("US-01, US-03, US-15"),
                        new Tag().name("Parcelles").description("US-02, US-03"),
                        new Tag().name("Campagnes").description("US-04"),
                        new Tag().name("Cultures").description("US-05, US-06, US-07"),
                        new Tag().name("Stock").description("US-08, US-09"),
                        new Tag().name("Offres").description("US-10, US-18"),
                        new Tag().name("Commandes").description("US-11, US-12, US-13, US-16"),
                        new Tag().name("Paiements").description("US-13, US-14"),
                        new Tag().name("Référentiel").description("US-18, US-19")));
    }

    /** Ajoute à chaque opération les erreurs communes à toutes : 401 et 500. */
    @Bean
    OpenApiCustomizer erreursCommunes() {
        return api -> api.getPaths().values().forEach(chemin -> chemin.readOperations().forEach(operation -> {
            operation.getResponses().addApiResponse("401", new ApiResponse().$ref("#/components/responses/NonAuthentifie"));
            operation.getResponses().addApiResponse("500", new ApiResponse().$ref("#/components/responses/ErreurInterne"));
        }));
    }

    private static ApiResponse erreur(String description) {
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/problem+json", new MediaType().schema(new Schema<>().$ref(SCHEMA_ERREUR))));
    }

}
