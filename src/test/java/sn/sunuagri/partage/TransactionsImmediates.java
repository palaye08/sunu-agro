package sn.sunuagri.partage;

import sn.sunuagri.partage.application.Transactions;

import java.util.function.Supplier;

/** Double de test du port Transactions : exécute directement le traitement. */
public class TransactionsImmediates implements Transactions {

    @Override
    public <T> T executer(Supplier<T> traitement) {
        return traitement.get();
    }
}
