package sn.sunuagri.commercialisation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Horloge de test qui avance d'une minute à chaque lecture (ordre chronologique déterministe). */
public class HorlogeReglable extends Clock {

    private Instant courant = Instant.parse("2026-10-01T08:00:00Z");

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        courant = courant.plus(Duration.ofMinutes(1));
        return courant;
    }
}
