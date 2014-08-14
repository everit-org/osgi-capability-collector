package org.everit.osgi.servicecollector;

import java.util.Map;

import org.osgi.framework.ServiceReference;

public interface ServiceCollectorListener {

    void addingService(String referenceName, String clause, ServiceReference<?> serviceReference,
            Map<String, ?> attributes, boolean satisfied);

    void removedService(String referenceName, String clause, boolean satisfied);
}
