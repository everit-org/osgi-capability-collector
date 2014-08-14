package org.everit.osgi.servicecollector;

import org.everit.osgi.servicecollector.internal.ServiceCollectorServiceTracker;
import org.osgi.framework.BundleContext;

public class ServiceCollector {

    private BundleContext bundleContext;
    private ServiceCollectorServiceTracker serviceCollectorServiceTracker;

    public ServiceCollector(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void addClause(String referenceName, Class<?> clazz, String clause) {

    }

    public void removeClause(String referenceName, Class<?> clazz, String clause) {

    }

    public void start() {
        serviceCollectorServiceTracker = new ServiceCollectorServiceTracker(bundleContext);
        serviceCollectorServiceTracker.open(true);
    }

    public void stop() {

    }
}
