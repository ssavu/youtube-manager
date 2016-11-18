package org.jahia.modules.youtubemanager.services.auth;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.youtube.model.Video;
import org.jahia.modules.youtubemanager.models.VideoDetails;
import org.jahia.modules.youtubemanager.models.YoutubeCredential;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by stefan on 2016-11-14.
 */
public interface YTMAuthService {

    public void initializeService(String clientId, String clientSecret, String scope, String callback);

    public YoutubeCredential initializeServiceAndProcess(YoutubeCredential youtubeCredential) throws IOException;

    public YoutubeCredential generateAuthorizationCredential(String code) throws IOException;

    public String getAuthorizationUrl(final Map<String, String> additionalParams);

    public Video uploadVideo(InputStream inputStream, String locale, VideoDetails videoDetails, HttpServletRequest req) throws IOException;

    public YoutubeCredential refreshAccessToken() throws IOException;
}
