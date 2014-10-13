/**
 * This file is part of Everit - OSGi Capability Collector.
 *
 * Everit - OSGi Capability Collector is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Capability Collector is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Capability Collector.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.capabilitycollector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceReferenceCollector<S> extends AbstractCapabilityCollector<ServiceReference<S>> {

    private class ReferenceTrackerCustomizer implements ServiceTrackerCustomizer<S, ServiceReference<S>> {

        @Override
        public ServiceReference<S> addingService(final ServiceReference<S> reference) {
            addingCapablility(reference);
            return reference;
        }

        @Override
        public void modifiedService(final ServiceReference<S> reference, final ServiceReference<S> tracked) {
            modifiedCapablility(reference);
        }

        @Override
        public void removedService(final ServiceReference<S> reference, final ServiceReference<S> tracked) {
            removedCapability(reference);
        }
    }

    private final boolean trackAllServices;

    private final ServiceTracker<S, ServiceReference<S>> tracker;

    public ServiceReferenceCollector(final BundleContext context, final Class<S> referenceType,
            final RequirementDefinition<ServiceReference<S>>[] items,
            final CapabilityConsumer<ServiceReference<S>> capabilityConsumer, final boolean trackAllServices) {
        super(context, items, capabilityConsumer);

        this.trackAllServices = trackAllServices;
        if (referenceType == null) {
            try {
                tracker = new ServiceTracker<S, ServiceReference<S>>(context, context.createFilter("(service.id=*)"),
                        new ReferenceTrackerCustomizer());
            } catch (InvalidSyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            tracker = new ServiceTracker<S, ServiceReference<S>>(context, referenceType,
                    new ReferenceTrackerCustomizer());
        }
    }

    @Override
    protected void closeTracker() {
        tracker.close();
    }

    @Override
    protected ServiceReference<S>[] getAvailableCapabilities() {
        return tracker.getServiceReferences();
    }

    @Override
    protected boolean matches(final ServiceReference<S> capability, final Filter filter) {
        return (filter == null) || filter.match(capability);
    }

    @Override
    protected void openTracker() {
        tracker.open(trackAllServices);
    }
}
