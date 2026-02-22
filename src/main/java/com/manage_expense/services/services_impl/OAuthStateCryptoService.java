package com.manage_expense.services.services_impl;

import com.manage_expense.helper.OAuth.OAuthState;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class OAuthStateCryptoService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;          // 96-bit IV (recommended for GCM)
    private static final int TAG_SIZE = 128;        // 128-bit auth tag
    private static final int MIN_PAYLOAD_SIZE = IV_SIZE + 16; // IV + GCM tag

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${security.oauth.state-key}")
    private String hexKey;

    private SecretKeySpec keySpec;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    void init() {
        byte[] keyBytes = HexFormat.of().parseHex(hexKey);

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("Invalid AES key length for OAuth state");
        }

        keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public <T> String encrypt(T state) {
        try {
            byte[] iv = new byte[IV_SIZE];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(TAG_SIZE, iv)
            );

            byte[] encrypted = cipher.doFinal(
                    mapper.writeValueAsBytes(state)
            );

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(buffer.array());

        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt OAuth state", e);
        }
    }

    public OAuthState decrypt(String state) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(state);

            if (data.length < MIN_PAYLOAD_SIZE) {
                throw new IllegalArgumentException("Invalid OAuth state payload");
            }

            ByteBuffer buffer = ByteBuffer.wrap(data);

            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(TAG_SIZE, iv)
            );

            byte[] json = cipher.doFinal(cipherText);
            return mapper.readValue(json, OAuthState.class);

        } catch (AEADBadTagException e) {
            // Authentication failed → tampered, expired, or wrong key
            throw new IllegalArgumentException("Invalid or expired OAuth state", e);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt OAuth state", e);
        }
    }
}
