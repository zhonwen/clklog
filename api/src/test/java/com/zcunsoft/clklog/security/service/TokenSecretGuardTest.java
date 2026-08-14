package com.zcunsoft.clklog.security.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

class TokenSecretGuardTest {

    @Test
    void rejectShortSecret() {
        Assertions.assertThrows(IllegalStateException.class, () -> TokenService.requireHs512Secret("too-short"));
        byte[] ok = new byte[64];
        Arrays.fill(ok, (byte) 'A');
        TokenService.requireHs512Secret(new String(ok, StandardCharsets.UTF_8));
    }

    @Test
    void applicationYmlMustUseEnvPlaceholders() throws Exception {
        String yml = new String(Files.readAllBytes(Paths.get("src/main/resources/application.yml")), StandardCharsets.UTF_8);
        Assertions.assertTrue(yml.contains("secret: ${TOKEN_SECRET}"));
        Assertions.assertTrue(yml.contains("password: ${CLICKHOUSE_PASSWORD}"));
        Assertions.assertTrue(yml.contains("password: ${REDIS_PASSWORD}"));
        Assertions.assertFalse(yml.contains("password: 123456"));
    }
}
