package sn.sunuagri.partage.application;

import java.util.function.Supplier;

/**
 * Port de sortie : unité de travail transactionnelle.
 * Garde la couche application indépendante du framework de persistance.
 */
public interface Transactions {

    <T> T executer(Supplier<T> traitement);

    default void executer(Runnable traitement) {
        executer(() -> {
            traitement.run();
            return null;
        });
    }
}
