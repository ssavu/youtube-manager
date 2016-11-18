package org.jahia.modules.youtubemanager;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author stefan on 2016-10-28.
 */
public class ApiApplication extends Application {

    private BundleContext context;
    private final Set<Object> singletons;

    public ApiApplication(BundleContext context) {
        this.context = context;
        ServiceReference ref = this.context.getServiceReference(Api.class.getName());
        //Add Main Resource
        Api api = (Api)this.context.getService(ref);
        singletons = new HashSet<>();
        singletons.add(api);
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        // register resources and features
        classes.add(MultiPartFeature.class);
        classes.add(LoggingFilter.class);
        return classes;
    }
}
