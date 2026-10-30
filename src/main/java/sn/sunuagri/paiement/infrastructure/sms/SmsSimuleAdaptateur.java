package sn.sunuagri.paiement.infrastructure.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sn.sunuagri.paiement.application.EnvoiSms;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptateur SIMULÉ d'envoi de SMS (l'envoi réel est hors périmètre).
 * Le SMS est écrit dans le journal de l'application. Les numéros listés dans
 * {@code sunuagri.sms.simulation.numeros-en-echec} simulent une panne de l'opérateur.
 */
@Component
public class SmsSimuleAdaptateur implements EnvoiSms {

    private static final Logger LOG = LoggerFactory.getLogger(SmsSimuleAdaptateur.class);

    private final Set<String> numerosEnEchec;

    public SmsSimuleAdaptateur(@Value("${sunuagri.sms.simulation.numeros-en-echec:}") String numerosEnEchec) {
        this.numerosEnEchec = Arrays.stream(numerosEnEchec.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    @Override
    public void envoyer(String numero, String message) {
        if (numerosEnEchec.contains(numero)) {
            throw new EchecEnvoiSms("Opérateur SMS injoignable (simulation) pour le numéro " + numero + ".");
        }
        LOG.info("[SMS SIMULÉ] à {} : {}", numero, message);
    }
}
