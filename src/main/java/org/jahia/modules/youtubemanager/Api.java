package org.jahia.modules.youtubemanager;

import org.jahia.modules.youtubemanager.subresources.Playlist;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author by stefan on 2016-10-28.
 */
@Component(service = Api.class)
@Path("/youtube-manager")
@Produces({"application/hal+json"})
public class Api {
    private final static Logger logger = LoggerFactory.getLogger(Api.class);
    private Playlist playlist;
    @Activate
    public void activate(BundleContext context) {
        ServiceReference ref = context.getServiceReference(Playlist.class.getName());
        playlist = (Playlist)context.getService(ref);
    }
    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHello() {
        return Response.status(Response.Status.OK).entity("{\"success\":\"Hello from Youtube Manager API!\"}").build();
    }

    @Path(Playlist.SUBRESOURCE)
    public Playlist getPlaylistSubResource() {
        return playlist;
    }
}
