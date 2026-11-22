// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncodingTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Test
    void bcryptHashesAndVerifiesPasswordWithoutStoringPlainText() {
        // TEST ONLY - synthetic password.
        String rawPassword = "test-only-strong-password";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword)
                .isNotEqualTo(rawPassword)
                .startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", encodedPassword)).isFalse();
    }
}
