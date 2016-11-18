package org.jahia.modules.youtubemanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import org.jahia.modules.youtubemanager.YTMHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by stefan on 2016-11-14.
 */
public class YoutubeCredential implements CredentialRefreshListener{

    private final static Logger logger = LoggerFactory.getLogger(YoutubeCredential.class);
    private String clientId;
    private String clientSecret;
    private String scope;
    private String callback;
    private YTMHttpClient httpClient;
    private JsonFactory jsonFactory;
    private NetHttpTransport netHttpTransport;
    private String accessToken;
    private String refreshToken;

    private GoogleCredential googleCredential;

    public YoutubeCredential() {

    }

    public YoutubeCredential(String clientId, String clientSecret,
                             String scope, String callback){
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.callback = callback;
        this.httpClient = new YTMHttpClient();
        this.jsonFactory = new JacksonFactory();
        this.netHttpTransport = new NetHttpTransport();
    }

    public String getClientId() {
        return clientId;
    }


    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }


    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }


    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonIgnore
    public YTMHttpClient getHttpClient() {
        return httpClient;
    }

//    @JsonIgnore
//    public void setHttpClient(YTMHttpClient httpClient) {
//        this.httpClient = httpClient;
//    }
    @JsonIgnore
    public JsonFactory getJsonFactory() {
        return jsonFactory;
    }
//    @JsonIgnore
//    public void setJsonFactory(JsonFactory jsonFactory) {
//        this.jsonFactory = jsonFactory;
//    }
    @JsonIgnore
    public NetHttpTransport getNetHttpTransport() {
        return netHttpTransport;
    }
//    @JsonIgnore
//    public void setNetHttpTransport(NetHttpTransport netHttpTransport) {
//        this.netHttpTransport = netHttpTransport;
//    }
    @JsonIgnore
    public GoogleCredential getGoogleCredential() {
        return googleCredential;
    }
//    @JsonIgnore
//    public void setGoogleCredential(GoogleCredential googleCredential) {
//        this.googleCredential = googleCredential;
//    }

    @JsonIgnore
    public String getJson() {
        ObjectMapper mapper = new ObjectMapper();
        final JsonNode classToJson = mapper.valueToTree(this);
        return classToJson != null ? classToJson.toString() : null;
    }

    @JsonIgnore
    public GoogleCredential createGoogleCredential() {
        if (googleCredential != null) {
            return googleCredential;
        }

        if (this.getHttpClient() == null) {
            this.httpClient = new YTMHttpClient();
        }
        if (this.getNetHttpTransport() == null) {
            this.netHttpTransport = new NetHttpTransport();
        }
        if (this.getJsonFactory() == null) {
            this.jsonFactory = new JacksonFactory();
        }
        List<CredentialRefreshListener> list = new LinkedList<>();
        list.add(this);
        googleCredential = new GoogleCredential.Builder()
                .setClientSecrets(this.getClientId(), this.getClientSecret())
                .setJsonFactory(this.getJsonFactory())
                .setTransport(this.getNetHttpTransport())
                .setRefreshListeners(list)
                .build()
                .setAccessToken(this.getAccessToken())
                .setRefreshToken(this.getRefreshToken());
        return googleCredential;
    }

    @JsonIgnore
    public YouTube createYouTube(String appName) {
        return new YouTube.Builder(this.getNetHttpTransport(), this.getJsonFactory(), createGoogleCredential())
                .setApplicationName(appName).build();
    }

    @JsonIgnore
    @Override
    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
        accessToken = credential.getAccessToken();
        logger.info("Refreshed token successfully");
    }
    @JsonIgnore
    @Override
    public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
        logger.info("Failed to refresh token");
        googleCredential = null;
    }
}
