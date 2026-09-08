package com.mini.credit.entity.referentiel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActivationToken - expiration")
class ActivationTokenTest {

    @Test
    @DisplayName("onCreate_shouldSetExpirationTo48Hours")
    void onCreate_shouldSetExpirationTo48Hours() {
        ActivationToken token = new ActivationToken();

        token.onCreate();

        Duration validity = Duration.between(token.getDateCreation(), token.getDateExpiration());
        assertThat(validity).isBetween(Duration.ofHours(47).plusMinutes(59), Duration.ofHours(48).plusMinutes(1));
    }
}