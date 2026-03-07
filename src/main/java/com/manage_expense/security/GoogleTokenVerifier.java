package com.manage_expense.security;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.manage_expense.config.AppConstants;
import com.manage_expense.config.GoogleOAuthProperties;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleTokenVerifier {

    private static final JsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(GoogleOAuthProperties properties) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new ApacheHttpTransport(),
                JSON_FACTORY
        )
                .setAudience(Collections.singletonList(properties.getClientId()))
                .setIssuer(AppConstants.GOOGLE_ISSUER)
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenRequest) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenRequest);

            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google ID token", e);
        }
    }
}