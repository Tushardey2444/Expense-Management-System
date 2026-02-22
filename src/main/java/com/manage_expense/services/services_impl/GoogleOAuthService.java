package com.manage_expense.services.services_impl;

import com.manage_expense.config.AppConstants;
import com.manage_expense.config.GoogleOAuthProperties;
import com.manage_expense.dtos.dto_responses.GoogleTokenResponse;
import com.manage_expense.dtos.dto_responses.GoogleUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties properties;

    public GoogleTokenResponse exchangeCode(String code, String verifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = getMultiValueMapHttpEntity(code, verifier, headers);

        try{
            GoogleTokenResponse response = restTemplate.postForObject(
                    AppConstants.GOOGLE_TOKEN_URI,
                    request,
                    GoogleTokenResponse.class
            );

            if (response == null || response.getAccessToken() == null || response.getIdToken() == null) {
                throw new IllegalStateException("Failed to exchange Google authorization code");
            }

            return response;
        }catch (HttpClientErrorException e){
            throw new IllegalStateException("Failed to exchange Google authorization code: " + e.getResponseBodyAsString());
        }
    }

    private HttpEntity<MultiValueMap<String, String>> getMultiValueMapHttpEntity(String code, String verifier, HttpHeaders headers) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("redirect_uri", properties.getRedirectUri());
        body.add("grant_type", AppConstants.GOOGLE_GRANT_TYPE);
        body.add("code_verifier", verifier);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);
        return request;
    }

    public String buildGoogleUrl(String encryptedState, String challenge) {
        return UriComponentsBuilder
                .fromUriString(properties.getAuthUri())
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email") // single param
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", encryptedState)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .encode() // REQUIRED to encode spaces used in scope
                .build()
                .toUriString();
    }

    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return restTemplate.exchange(
                AppConstants.GOOGLE_USERINFO_URI,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GoogleUserInfoResponse.class
        ).getBody();
    }
}

/*
        body.add("grant_type", "refresh_token");
        Used when:
            1. Access token expired
            2. You want a new access token without re-login
*/
