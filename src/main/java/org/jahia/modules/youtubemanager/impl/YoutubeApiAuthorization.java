package org.jahia.modules.youtubemanager.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.youtubemanager.models.VideoDetails;
import org.jahia.modules.youtubemanager.models.YoutubeCredential;
import org.jahia.modules.youtubemanager.services.YTMService;
import org.jahia.modules.youtubemanager.services.auth.YTMAuthService;
import org.jahia.modules.youtubemanager.util.YoutubeEnums;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.URLGenerator;
import org.jahia.services.render.URLResolverFactory;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by stefan on 2016-10-28.
 */
public class YoutubeApiAuthorization extends YTMService {
    private static final Logger logger = LoggerFactory.getLogger(YoutubeApiAuthorization.class);
    private static final String YOUTUBE_CREDENTIAL = "youtube_credential";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String YOUTUBE_MANAGER_PATH = "youtubeManager";
    private static final String YOUTUBE_CREDENTIAL_NODE_PATH = "ytm:youtubeAuthCredential";
    private static final String YOUTUBE_CREDENTIAL_PROPERTY = "ytm:credential";
    private YTMAuthService ytmAuthManager;

    public YoutubeApiAuthorization(JCRTemplate jcrTemplate,
                                   JahiaTemplateManagerService templateManagerService,
                                   URLResolverFactory urlResolverFactory, YTMAuthService ytmAuthManager) {
        super(jcrTemplate, templateManagerService, urlResolverFactory,logger);
        this.ytmAuthManager = ytmAuthManager;
    }

    public boolean getAuthorizationToken(String code, HttpServletRequest req, final String siteId) throws RepositoryException, IOException {
        final YoutubeCredential youtubeCredential = ytmAuthManager.generateAuthorizationCredential(code);
        if (youtubeCredential != null) {
            updateTokenInSessionAndJCR(youtubeCredential, req, siteId);
        } else {
            logger.info("Failed to get access token");
            return false;
        }
        return true;
    }

    public boolean isAuthorized(HttpServletRequest request,
                                String locale, String siteId) throws RepositoryException{
        //First try to get it from the sessions.
        boolean authorized;
        if (request.getSession().getAttribute(YOUTUBE_CREDENTIAL) != null) {
            authorized = true;
        } else {
            //Try get the google credential from the jcr
            JCRSessionWrapper session = getDefaultSession(locale);
            JCRSiteNode siteNode = (JCRSiteNode) session.getNodeByIdentifier(siteId);
            authorized = siteNode.hasNode(YOUTUBE_MANAGER_PATH + "/" + YOUTUBE_CREDENTIAL_NODE_PATH);
        }
        return authorized;
    }

    public String sendRedirect() {
        final Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("access_type", "offline");
        additionalParams.put("approval_prompt", "force");
        return ytmAuthManager.getAuthorizationUrl(additionalParams);
    }

    public boolean initializeServiceAndProcess(String locale,
                                            HttpServletRequest request,
                                            String siteId) throws RepositoryException, IOException {
        if (this.isAuthorized(request, locale, siteId)) {
            YoutubeCredential youtubeCredential;
            ObjectMapper mapper = new ObjectMapper();
            String youtubeCredentialJson = (String) request.getSession().getAttribute(YOUTUBE_CREDENTIAL);
            //Get the youtubeCredentialJson from Session or JCR
            if (youtubeCredentialJson != null) {
                youtubeCredential = mapper.readValue(youtubeCredentialJson, YoutubeCredential.class);
            } else {
                JCRSessionWrapper session = getDefaultSession(locale);

                JCRSiteNode siteNode = (JCRSiteNode) session.getNodeByIdentifier(siteId);
                JCRNodeWrapper youtubeCredentialNode = siteNode.getNode(YOUTUBE_MANAGER_PATH + "/" + YOUTUBE_CREDENTIAL_NODE_PATH);
                youtubeCredentialJson = youtubeCredentialNode.getPropertyAsString(YOUTUBE_CREDENTIAL_PROPERTY);
                youtubeCredential = mapper.readValue(youtubeCredentialJson, YoutubeCredential.class);
            }
            youtubeCredential = ytmAuthManager.initializeServiceAndProcess(youtubeCredential);
            if (youtubeCredential != null) {
                updateTokenInSessionAndJCR(youtubeCredential, request, siteId);
            }
            return true;
        }
        return false;
    }
    public String initializeService(String locale,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  String siteId,
                                  String data,
                                  String scopes) throws IOException, JSONException, RepositoryException{
        //@TODO add exception handling for missing parameters
        JSONObject jsonObject = new JSONObject(data);
        JSONObject jsonConfigObj = jsonObject.getJSONObject("web");

        String clientId = jsonConfigObj.getString(CLIENT_ID);
        String clientSecret = jsonConfigObj.getString(CLIENT_SECRET);
        JCRSessionWrapper session = getDefaultSession(locale);
        JCRNodeWrapper siteNode = session.getNodeByIdentifier(siteId);
        RenderContext renderContext = createRenderContext(request, response, session, siteNode);
        URLGenerator urlGenerator = new URLGenerator(renderContext, renderContext.getMainResource());
        StringBuilder uri = new StringBuilder(urlGenerator.getServer());
        uri.append(urlGenerator.getContext());
        uri.append(urlGenerator.getBaseEdit());
        String angularTemplatePath = urlGenerator.getTemplate("youtube-manager");
        uri.append(angularTemplatePath.substring(angularTemplatePath.indexOf("/sites/")));
        String[] selectedScopeTypes = StringUtils.split(scopes, ",");
        StringBuilder scopesArg = new StringBuilder();
        for (int i = 0; i < selectedScopeTypes.length; i++) {
            scopesArg.append(YoutubeEnums.AuthorizationScopeTypes.build(selectedScopeTypes[i]));
            if (i + 1 < selectedScopeTypes.length) {
                scopesArg.append(" ");
            }
        }
        ytmAuthManager.initializeService(clientId, clientSecret,
                scopesArg.toString(),
                uri.toString());
        return sendRedirect();
    }

    public boolean uploadVideo(InputStream inputStream,
                            String locale,
                            String data,
                            HttpServletRequest req,
                            String siteId) throws JSONException, IOException, RepositoryException {
        ObjectMapper mapper = new ObjectMapper();
        VideoDetails videoDetails = mapper.readValue(data, VideoDetails.class);
        ytmAuthManager.uploadVideo(inputStream, locale, videoDetails, req);
        return true;
    }

    private boolean updateTokenInSessionAndJCR(final YoutubeCredential youtubeCredential, HttpServletRequest req, final String siteId) throws RepositoryException {
        final String youtubeCredentialJson = youtubeCredential.getJson();
        //Save in jcr and set in session
        JCRCallback callback = new JCRCallback() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRSiteNode siteNode = (JCRSiteNode) session.getNodeByIdentifier(siteId);
                JCRNodeWrapper youtubeNode = !siteNode.hasNode(YOUTUBE_MANAGER_PATH) ? siteNode.addNode(YOUTUBE_MANAGER_PATH, "ytm:youtubeManager") : siteNode.getNode(YOUTUBE_MANAGER_PATH);
                session.checkout(youtubeNode);
                JCRNodeWrapper googleCredentialNode = !youtubeNode.hasNode(YOUTUBE_CREDENTIAL_NODE_PATH) ? youtubeNode.addNode(YOUTUBE_CREDENTIAL_NODE_PATH, "ytm:youtubeAuthCredential") : youtubeNode.getNode(YOUTUBE_CREDENTIAL_NODE_PATH);
                googleCredentialNode.setProperty(YOUTUBE_CREDENTIAL_PROPERTY, youtubeCredentialJson);
                session.save();
                return true;
            }
        };
        jcrTemplate.doExecuteWithSystemSession(callback);
        req.getSession().setAttribute(YOUTUBE_CREDENTIAL, youtubeCredentialJson);
        logger.info("Access token is: " + youtubeCredential.getAccessToken());
        return true;
    }
}
