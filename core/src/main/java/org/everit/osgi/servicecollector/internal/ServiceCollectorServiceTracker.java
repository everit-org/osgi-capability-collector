package org.everit.osgi.servicecollector.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ServiceCollectorServiceTracker extends ServiceTracker<Object, Object> {

    private static final Filter FILTER;

    static {
        try {
            FILTER = FrameworkUtil.createFilter("(service.id=*)");
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public ServiceCollectorServiceTracker(BundleContext context) {
        super(context, FILTER, null);
    }

    @Override
    public Object addingService(ServiceReference<Object> reference) {
        // TODO Auto-generated method stub
        return super.addingService(reference);
    }

    @Override
    public void modifiedService(ServiceReference<Object> reference, Object service) {
        // TODO Auto-generated method stub
        super.modifiedService(reference, service);
    }

    @Override
    public void removedService(ServiceReference<Object> reference, Object service) {
        // TODO Auto-generated method stub
        super.removedService(reference, service);
    }
}
