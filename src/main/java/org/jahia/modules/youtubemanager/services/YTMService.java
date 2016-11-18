package org.jahia.modules.youtubemanager.services;

import org.jahia.api.Constants;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolverFactory;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.utils.LanguageCodeConverters;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by stefan on 2016-10-28.
 */
public class YTMService {
    protected Logger logger;
    protected JCRTemplate jcrTemplate;
    protected JahiaTemplateManagerService templateManagerService;
    protected URLResolverFactory urlResolverFactory;

    protected YTMService(JCRTemplate jcrTemplate,
                         JahiaTemplateManagerService templateManagerService,
                         URLResolverFactory urlResolverFactory, Logger logger) {
        this.jcrTemplate = jcrTemplate;
        this.templateManagerService = templateManagerService;
        this.urlResolverFactory = urlResolverFactory;
        this.logger = logger;
    }

    /**
     * This method creates a session
     *
     * @param language : String current language
     * @return JCRSessionWrapper
     */
    protected JCRSessionWrapper getDefaultSession(String language) throws RepositoryException {
        return jcrTemplate.getSessionFactory().getCurrentUserSession(Constants.EDIT_WORKSPACE, LanguageCodeConverters.getLocaleFromCode(language));
    }

    /**
     * This method creates a system session
     *
     * @param language : String current language
     * @return JCRSessionWrapper
     */
    protected JCRSessionWrapper getSystemSession(String language) throws RepositoryException {
        return jcrTemplate.getSessionFactory().getCurrentSystemSession(Constants.EDIT_WORKSPACE, LanguageCodeConverters.getLocaleFromCode(language), LanguageCodeConverters.getLocaleFromCode(language));
    }

    /**
     * Returns default sessions with no locale
     *
     * @return JCRSessionWrapper
     * @throws RepositoryException
     */
    protected JCRSessionWrapper getDefaultSession() throws RepositoryException {
        return jcrTemplate.getSessionFactory().getCurrentUserSession(Constants.EDIT_WORKSPACE);
    }

    /**
     * Creates or gets a node (borrowed from Kevan)
     *
     * @param parentNode
     * @param name
     * @param type
     * @return
     * @throws RepositoryException
     */
    protected JCRNodeWrapper getOrCreateNode(JCRNodeWrapper parentNode, String name, String type) throws RepositoryException {
        JCRNodeWrapper node;
        if (!parentNode.hasNode(name)) {
            node = parentNode.addNode(name, type);
        } else {
            node = parentNode.getNode(name);
        }
        return node;
    }

    /**
     * This method will create a valid renderContext.
     *
     * @param request  : HttpServletRequest request
     * @param response : HttpServletResponse response
     * @param session  : current user session
     * @param node     : node representing main resource
     * @return RenderContext
     */
    protected RenderContext createRenderContext(HttpServletRequest request, HttpServletResponse response, JCRSessionWrapper session, JCRNodeWrapper node) {
        RenderContext renderContext = new RenderContext(request, response, session.getUser());
        Resource mainResource = new Resource(node, "html", null, Resource.CONFIGURATION_PAGE);
        renderContext.setMainResource(mainResource);
        renderContext.setServletPath(Render.getRenderServletPath());
        try {
            renderContext.setSite(node.getResolveSite());
        } catch (RepositoryException e) {
            logger.error("Unable to resolve site. " + e.getMessage());
            return null;
        }
        return renderContext;
    }
}
