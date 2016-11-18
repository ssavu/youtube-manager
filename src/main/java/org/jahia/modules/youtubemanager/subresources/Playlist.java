package org.jahia.modules.youtubemanager.subresources;


import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.modules.youtubemanager.impl.YoutubeApiAuthorization;
import org.jahia.modules.youtubemanager.services.auth.YTMAuthManager;
import org.jahia.modules.youtubemanager.services.auth.YTMAuthService;
import org.jahia.modules.youtubemanager.util.YoutubeEnums;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.URLResolverFactory;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author by stefan on 2016-10-28.
 */
@Component(service = Playlist.class)
public class Playlist {
    public static final String SUBRESOURCE = "playlist";
    private static final Logger logger = LoggerFactory.getLogger(Playlist.class);
    private YoutubeApiAuthorization youtubeApiAuthorization;
    private JCRTemplate jcrTemplate;
    private JahiaTemplateManagerService templateManagerService;
    private URLResolverFactory urlResolverFactory;
    private YTMAuthService ytmAuthManager;
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.jcrTemplate = (JCRTemplate) getService(JCRTemplate.class, bundleContext);
        this.templateManagerService = (JahiaTemplateManagerService) getService(JahiaTemplateManagerService.class, bundleContext);
        this.urlResolverFactory = (URLResolverFactory) getService(URLResolverFactory.class, bundleContext);
        this.ytmAuthManager = (YTMAuthService) getService(YTMAuthService.class, bundleContext);
        this.youtubeApiAuthorization = new YoutubeApiAuthorization(jcrTemplate,
                templateManagerService, urlResolverFactory, ytmAuthManager);
    }

    public Object getService(Class classObj, BundleContext bundleContext) {
        ServiceReference ref = bundleContext.getServiceReference(classObj.getName());
        return ref != null ? bundleContext.getService(ref) : ref;
    }

    @POST
    @Path("/video/upload/{locale}/{siteId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadVideo(@PathParam("locale") String locale,
                                @PathParam("siteId") String siteId,
                                @Context HttpServletRequest request,
                                @Context HttpServletResponse response,
                                @FormDataParam("file") InputStream inputStream,
                                @FormDataParam("videoDetails") String videoDetails) {
        boolean uploadedSuccessfully;
        try {
            uploadedSuccessfully = youtubeApiAuthorization.uploadVideo(inputStream, locale, videoDetails, request, siteId);
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        } catch (IOException e) {
            logger.error("Failed to read or write file: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to read or write file\"}").build();
        } catch (RepositoryException e) {
            logger.error("Could not read from JCR: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Could not read from JCR\"}").build();

        }

        return Response.status(Response.Status.OK).entity("{\"result\":" + uploadedSuccessfully + "}").build();
    }

    @GET
    @Path("/authorize/verify/{locale}/{siteId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyAccessCode(@Context HttpServletRequest request,
                                     @PathParam("locale") String locale,
                                     @PathParam("siteId") String siteId) {
        try {
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("authorized", youtubeApiAuthorization.isAuthorized(request, locale, siteId));
            return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        } catch (RepositoryException e) {
            logger.error("Could not read from JCR: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Could not read from JCR\"}").build();

        }
    }

    @POST
    @Path("/authorize/process/{siteId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processAuthorization(@Context HttpServletRequest request,
                                         @Context HttpServletResponse response,
                                         @PathParam("siteId") String siteId,
                                         String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            String code = jsonObject.getString("code");
            if (StringUtils.isEmpty(code)) {
                logger.error("Failed to request access token due to missing parameter: 'code'.");
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Failed to request access token due to missing parameter: 'code'.\"}").build();
            }
            JSONObject answer = new JSONObject();
            answer.put("authorized", youtubeApiAuthorization.getAuthorizationToken(code, request, siteId));
            return Response.status(Response.Status.OK).entity(answer.toString()).build();
        }  catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        } catch (RepositoryException e) {
            logger.error("Could not read from JCR: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Could not read from JCR\"}").build();

        } catch (IOException e) {
            logger.error("Failed to read or write file: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to read or write file\"}").build();
        }
    }

    @POST
    @Path("/authorize/initialize/{locale}/{siteId}/{scopes}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response initializeAuthorization(@Context HttpServletRequest request,
                                            @Context HttpServletResponse response,
                                            @PathParam("locale") String locale,
                                            @PathParam("siteId") String siteId,
                                            @PathParam("scopes") String scopes,
                                            String data) {
        try {
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("result", youtubeApiAuthorization.initializeService(locale, request, response, siteId, data, scopes));
            return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
        } catch (RepositoryException e) {
            logger.error("Could not read from JCR: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Could not read from JCR\"}").build();

        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        } catch (IOException e) {
            logger.error("Failed to read file: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to read file\"}").build();
        }
    }

    @POST
    @Path("/authorize/reinitialize/{locale}/{siteId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reinitializeAuthorization(@Context HttpServletRequest request,
                                            @PathParam("locale") String locale,
                                            @PathParam("siteId") String siteId) {
        try {
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("result", youtubeApiAuthorization.initializeServiceAndProcess(locale, request, siteId));
            return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
        } catch (RepositoryException e) {
            logger.error("Could not read from JCR: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Could not read from JCR\"}").build();
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        } catch (IOException e) {
            logger.error("Failed to read file: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Failed to read file\"}").build();
        }
    }

    @GET
    @Path("/scopes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableScopes() {
        try {
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("scopes", YoutubeEnums.getAvailableTypes(YoutubeEnums.AuthorizationScopeTypes.class));
            return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        }
    }

    @GET
    @Path("/video/formats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableVideoFormats() {
        try {
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("formats", YoutubeEnums.getAvailableTypes(YoutubeEnums.VideoFormat.class));
            return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        }
    }

    @GET
    @Path("/video/statustypes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableVideoStatusTypes() {
        try {
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("statustypes", YoutubeEnums.getAvailableTypes(YoutubeEnums.VideoStatus.class));
            return Response.status(Response.Status.OK).entity(jsonAnswer.toString()).build();
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        }
    }

    @GET
    @Path("/reset/{siteId}")
    public Response reset(@Context HttpServletRequest request,
                          @PathParam("siteId") final String siteId) {
        request.getSession().removeAttribute("YOUTUBE_CREDENTIAL");
        //Save in jcr and set in session
        JCRCallback callback = new JCRCallback() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRSiteNode siteNode = (JCRSiteNode) session.getNodeByIdentifier(siteId);
                if (!siteNode.hasNode("youtubeManager")) {
                    return true;
                }
                JCRNodeWrapper youtubeNode = siteNode.getNode("youtubeManager");
                session.checkout(youtubeNode);
                if (!youtubeNode.hasNode("ytm:youtubeAuthCredential")) {
                    return true;
                }
                JCRNodeWrapper googleCredentialNode = youtubeNode.getNode("ytm:youtubeAuthCredential");
                googleCredentialNode.remove();
                session.save();
                return true;
            }
        };
        try {
            jcrTemplate.doExecuteWithSystemSession(callback);
        } catch (RepositoryException e) {
            logger.error("Could not read from JCR: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Could not read from JCR\"}").build();
        }
        request.getSession().removeAttribute("youtube_credential");
        return Response.status(Response.Status.OK).entity("{\"success\":\"successfully reset app\"").build();
    }

    @POST
    @Path("/video/progress")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response videoUploadProgress(@Context HttpServletRequest request,
                                        String data) {
        JSONObject answer = new JSONObject();
        try {
            answer.put("result", request.getSession().getAttribute(YTMAuthManager.YOUTUBE_VIDEO_UPLOAD_PROGRESS));
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        }
        return Response.status(Response.Status.OK).entity(answer.toString()).build();
    }

    @POST
    @Path("/video/progress/clear/{videoUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response videoUploadProgressClear(@Context HttpServletRequest request,
                                             @PathParam("videoUUID") String videoUUID) {
        JSONObject answer = new JSONObject();
        try {
            Map<String, Map> videoUploadProgressHolder = (Map) request.getSession().getAttribute(YTMAuthManager.YOUTUBE_VIDEO_UPLOAD_PROGRESS);
            if (videoUploadProgressHolder.containsKey(videoUUID)) {
                videoUploadProgressHolder.remove(videoUUID);
            }
            answer.put("result", "Successfully removed video with UUID: " + videoUUID);
        } catch (JSONException e) {
            logger.error("Invalid json object: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"Invalid json object\"}").build();
        }
        return Response.status(Response.Status.OK).entity(answer.toString()).build();
    }
}

