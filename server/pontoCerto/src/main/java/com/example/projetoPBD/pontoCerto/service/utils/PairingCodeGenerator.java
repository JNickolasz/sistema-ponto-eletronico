package com.example.projetoPBD.pontoCerto.service.utils;


import com.example.projetoPBD.pontoCerto.valueobjects.PairingCodeVO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class PairingCodeGenerator {

    private final SecureRandom secureRandom;


    public PairingCodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public PairingCodeVO generate(int length) {
        if (length < 1 || length > 10) {
            throw new IllegalArgumentException(
                    "Length deve estar entre 1 e 10"
            );
        }

        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(10));
        }

        return new PairingCodeVO(code.toString());
    }

    public String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    code.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }


}
