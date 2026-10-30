package sn.sunuagri.paiement.domaine;

import sn.sunuagri.partage.domaine.Kilogrammes;
import sn.sunuagri.partage.domaine.MontantXof;
import sn.sunuagri.partage.domaine.RegleMetierEnfreinte;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de domaine (US-13) : répartit le montant d'une commande entre la coopérative et les producteurs.
 *
 * <p><b>Règle d'arrondi</b> (le franc CFA n'a pas de subdivision) :</p>
 * <ol>
 *   <li>Commission = montant × taux, arrondie au franc le plus proche (demi vers le haut).</li>
 *   <li>Montant à répartir = montant − commission.</li>
 *   <li>Chaque producteur reçoit d'abord la partie entière de (montant à répartir × sa quantité / quantité totale).</li>
 *   <li>Les francs restants (moins que le nombre de producteurs) sont attribués un par un aux producteurs ayant les
 *       plus grands restes de division (méthode du plus fort reste) ; à reste égal, au plus gros apport, puis
 *       par identifiant, pour un résultat déterministe.</li>
 * </ol>
 * <p>Ainsi, commission + somme des paiements = montant de la commande, exactement : aucun franc ne se perd ni n'apparaît.</p>
 */
public final class RepartitionPaiement {

    private RepartitionPaiement() {
    }

    public static Repartition repartir(MontantXof montantCommande, BigDecimal tauxCommission, List<Apport> apports) {
        Map<UUID, Kilogrammes> parProducteur = new LinkedHashMap<>();
        apports.forEach(a -> parProducteur.merge(a.producteurId(), a.quantite(), Kilogrammes::plus));
        long quantiteTotale = parProducteur.values().stream().mapToLong(Kilogrammes::valeur).sum();
        RegleMetierEnfreinte.verifier(quantiteTotale > 0, "REPARTITION_IMPOSSIBLE",
                "Aucune quantité apportée : impossible de répartir le paiement.");

        MontantXof commission = montantCommande.multiplierParTaux(tauxCommission);
        long aRepartir = montantCommande.moins(commission).valeur();

        record Calcul(UUID producteurId, Kilogrammes quantite, long base, BigInteger reste) {
        }
        List<Calcul> calculs = new ArrayList<>();
        long distribue = 0;
        for (Map.Entry<UUID, Kilogrammes> e : parProducteur.entrySet()) {
            BigInteger[] division = BigInteger.valueOf(aRepartir).multiply(BigInteger.valueOf(e.getValue().valeur()))
                    .divideAndRemainder(BigInteger.valueOf(quantiteTotale));
            long base = division[0].longValueExact();
            distribue += base;
            calculs.add(new Calcul(e.getKey(), e.getValue(), base, division[1]));
        }

        long francsRestants = aRepartir - distribue;
        List<Calcul> ordreAttribution = calculs.stream()
                .sorted(Comparator.comparing(Calcul::reste).reversed()
                        .thenComparing(Calcul::quantite, Comparator.reverseOrder())
                        .thenComparing(Calcul::producteurId))
                .toList();
        Map<UUID, Long> bonus = new LinkedHashMap<>();
        for (int i = 0; i < francsRestants; i++) {
            bonus.merge(ordreAttribution.get(i).producteurId(), 1L, Long::sum);
        }

        List<Repartition.Part> parts = calculs.stream()
                .map(c -> new Repartition.Part(c.producteurId(), c.quantite(),
                        MontantXof.de(c.base() + bonus.getOrDefault(c.producteurId(), 0L))))
                .toList();
        Repartition repartition = new Repartition(montantCommande, commission, parts);

        if (repartition.totalVerse().plus(commission).valeur() != montantCommande.valeur()) {
            throw new IllegalStateException("Invariant violé : la répartition ne conserve pas le montant.");
        }
        return repartition;
    }
}
