package sn.sunuagri.partage.application;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Utilisateur à l'origine d'une demande (identité simulée, cf. ADR-004).
 * Pour un producteur, l'identifiant est celui du producteur ; pour un acheteur,
 * c'est son identifiant d'acheteur. Les contrôles d'accès (US-17) sont faits
 * dans la couche application, quel que soit le canal d'entrée (E3).
 */
public record Utilisateur(Role role, String identifiant) {

    public Utilisateur {
        Objects.requireNonNull(role, "role");
    }

    public static Utilisateur agent() {
        return new Utilisateur(Role.AGENT, "agent");
    }

    public static Utilisateur administrateur() {
        return new Utilisateur(Role.ADMINISTRATEUR, "admin");
    }

    public static Utilisateur producteur(UUID producteurId) {
        return new Utilisateur(Role.PRODUCTEUR, producteurId.toString());
    }

    public static Utilisateur acheteur(String acheteurId) {
        return new Utilisateur(Role.ACHETEUR, acheteurId);
    }

    /** Refuse l'opération si l'utilisateur n'a aucun des rôles attendus. */
    public void exigerRole(Role... rolesAutorises) {
        if (Arrays.stream(rolesAutorises).noneMatch(r -> r == role)) {
            throw new AccesRefuse("Opération non autorisée pour le rôle " + role + ".");
        }
    }

    public boolean estAgent() {
        return role == Role.AGENT;
    }

    public boolean estAcheteur() {
        return role == Role.ACHETEUR;
    }

    /** Vrai si l'utilisateur est ce producteur précis. */
    public boolean estLeProducteur(UUID producteurId) {
        return role == Role.PRODUCTEUR && producteurId != null && producteurId.toString().equals(identifiant);
    }

    /** Vrai si l'utilisateur est cet acheteur précis. */
    public boolean estLAcheteur(String acheteurId) {
        return role == Role.ACHETEUR && Objects.equals(identifiant, acheteurId);
    }

    /** Un agent voit tout ; un producteur ne voit que ce qui lui appartient. */
    public boolean peutVoirDonneesDuProducteur(UUID producteurId) {
        return estAgent() || estLeProducteur(producteurId);
    }
}
