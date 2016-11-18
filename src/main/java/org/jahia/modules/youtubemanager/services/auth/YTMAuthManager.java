package org.jahia.modules.youtubemanager.services.auth;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import org.jahia.modules.youtubemanager.models.VideoDetails;
import org.jahia.modules.youtubemanager.models.YoutubeCredential;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by stefan on 2016-11-03.
 */
@Component(service = YTMAuthService.class)
public class YTMAuthManager implements YTMAuthService {
    private static final Logger logger = LoggerFactory.getLogger(YTMAuthManager.class);
    private OAuth20Service service = null;
    private YoutubeCredential youtubeCredential;
    private YouTube youtube;

    public final static String YOUTUBE_VIDEO_UPLOAD_PROGRESS = "youtubeVideoUploadProgress";

    public YoutubeCredential initializeServiceAndProcess(YoutubeCredential youtubeCredential) throws IOException{
        if(this.youtubeCredential == null) {
            this.youtubeCredential = new YoutubeCredential(youtubeCredential.getClientId(),
                    youtubeCredential.getClientSecret(), youtubeCredential.getScope(),
                    youtubeCredential.getCallback());
            this.youtubeCredential.setAccessToken(youtubeCredential.getAccessToken());
            this.youtubeCredential.setRefreshToken(youtubeCredential.getRefreshToken());
        }
        initializeService();
        if (this.youtube == null) {
            this.youtube = this.youtubeCredential.createYouTube("Web Client");
        }
        refreshAccessToken();
        return youtubeCredential;
    }

    public void initializeService(String clientId, String clientSecret,
                                  String scope, String callback) {
        youtubeCredential = new YoutubeCredential(clientId, clientSecret,
                scope, callback.toString());
        initializeService();
    }

    public void initializeService() {
        if (service == null) {
            service = new ServiceBuilder()
                    .apiKey(youtubeCredential.getClientId())
                    .apiSecret(youtubeCredential.getClientSecret())
                    .scope(youtubeCredential.getScope())
                    .callback(youtubeCredential.getCallback())
                    .httpClient(youtubeCredential.getHttpClient())
                    .build(GoogleApi20.instance());
        }
    }

    public YoutubeCredential generateAuthorizationCredential(String code) throws IOException {
        OAuth2AccessToken accessToken = service.getAccessToken(code);
        youtubeCredential.setAccessToken(accessToken.getAccessToken());
        youtubeCredential.setRefreshToken(accessToken.getRefreshToken());

        //@TODO store google credential in JCR or/and session.
        youtube = youtubeCredential.createYouTube("Web Client");
        return youtubeCredential;
    }

    public String getAuthorizationUrl(final Map<String, String> additionalParams) {
        return service.getAuthorizationUrl(additionalParams);
    }

    public Video uploadVideo(InputStream inputStream, String locale, final VideoDetails videoDetails, final HttpServletRequest req) throws IOException{
        Video videoMetaData = new Video();
        VideoStatus videoStatus = new VideoStatus();
        videoStatus.setPrivacyStatus(videoDetails.getStatus().getDisplayName());

        videoMetaData.setStatus(videoStatus);

        VideoSnippet videoSnippet = new VideoSnippet();

        videoSnippet.setTitle(videoDetails.getTitle());
        videoSnippet.setDescription(videoDetails.getDescription());
        videoSnippet.setTags(videoDetails.getTags());

        videoMetaData.setSnippet(videoSnippet);

        InputStreamContent inputStreamContent = new InputStreamContent(videoDetails.getFormat().getDisplayName(), new BufferedInputStream(inputStream));
        if (inputStreamContent.getLength() == -1 && videoDetails.getSize() != -1) {
            inputStreamContent.setLength(videoDetails.getSize());
        }
        // /@TODO Improve setting of information about API request(first parameter)
        //We can build the string based on what properties we have set.
        YouTube.Videos.Insert videoInsert = youtube.videos().insert("snippet,statistics,status", videoMetaData, inputStreamContent);

        MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

        //@TODO Can update to be apart of video details/upload options
        uploader.setDirectUploadEnabled(false);

        MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
            public void progressChanged(MediaHttpUploader uploader) throws IOException {
                switch (uploader.getUploadState()) {
                    case INITIATION_STARTED:
                        Map<String, Map> videoUploadProgressHolder = (Map) req.getSession().getAttribute(YOUTUBE_VIDEO_UPLOAD_PROGRESS);
                        if (videoUploadProgressHolder == null) {
                            videoUploadProgressHolder = new LinkedHashMap<>();
                        }
                        Map<String, Object> videoProgressDetails = new LinkedHashMap<>();
                        videoProgressDetails.put("bytesUploaded", 0);
                        videoProgressDetails.put("progress", 0);
                        videoProgressDetails.put("uploadComplete", false);
                        videoUploadProgressHolder.put(Long.toString(videoDetails.getUuid()), videoProgressDetails);
                        req.getSession().setAttribute(YOUTUBE_VIDEO_UPLOAD_PROGRESS, videoUploadProgressHolder);
                        logger.info("Initiation Started");
                        break;
                    case INITIATION_COMPLETE:
                        logger.info("Initiation Completed");
                        break;
                    case MEDIA_IN_PROGRESS:
                        logger.info("Upload in progress");
                        double bytesUploaded = uploader.getNumBytesUploaded();
                        logger.info("Bytes uploaded: " + bytesUploaded);
                        videoUploadProgressHolder = (Map) req.getSession().getAttribute(YOUTUBE_VIDEO_UPLOAD_PROGRESS);
                        videoProgressDetails = videoUploadProgressHolder.get(Long.toString(videoDetails.getUuid()));
                        try {
                            videoProgressDetails.put("bytesUploaded", bytesUploaded);
                            double progress = uploader.getProgress();
                            logger.info("Upload percentage: " + progress);
                            videoProgressDetails.put("progress", progress);
                        } catch(IllegalArgumentException ex) {
                            logger.warn("Could not determine upload percentage");
                        }
                        videoUploadProgressHolder.put(Long.toString(videoDetails.getUuid()), videoProgressDetails);
                        req.getSession().setAttribute(YOUTUBE_VIDEO_UPLOAD_PROGRESS, videoUploadProgressHolder);
                        break;
                    case MEDIA_COMPLETE:
                        logger.info("Upload Completed!");
                        videoUploadProgressHolder = (Map) req.getSession().getAttribute(YOUTUBE_VIDEO_UPLOAD_PROGRESS);
                        videoUploadProgressHolder.get(Long.toString(videoDetails.getUuid())).put("uploadComplete", true);
                        break;
                    case NOT_STARTED:
                        logger.info("Upload Not Started!");
                        break;
                }
            }
        };
        uploader.setProgressListener(progressListener);
        //@TODO store completed video information into JCR
        Video returnedVideo = videoInsert.execute();
        logger.info("Uploaded video ID: " + returnedVideo.getId());
        return returnedVideo;
    }

    public YoutubeCredential refreshAccessToken() throws IOException {
        if (youtubeCredential.getGoogleCredential().getExpiresInSeconds() == null || youtubeCredential.getGoogleCredential().getExpiresInSeconds() <= 0) {
            if (youtubeCredential.getGoogleCredential().refreshToken()) {
                return youtubeCredential;
            } else {
                return null;
            }
        }
        return youtubeCredential;
    }
}
