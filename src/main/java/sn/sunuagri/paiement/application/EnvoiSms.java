package sn.sunuagri.paiement.application;

/**
 * Port de sortie : envoi d'un SMS. L'adaptateur actuel est une simulation ;
 * un vrai fournisseur (Orange, etc.) se branche ici sans toucher aux règles métier (E2).
 */
public interface EnvoiSms {

    /** @throws EchecEnvoiSms si le SMS n'a pas pu être envoyé */
    void envoyer(String numero, String message);

    class EchecEnvoiSms extends RuntimeException {
        public EchecEnvoiSms(String motif) {
            super(motif);
        }
    }
}
