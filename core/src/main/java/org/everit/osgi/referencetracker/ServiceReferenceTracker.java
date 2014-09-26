/**
 * This file is part of Everit - OSGi Reference Tracker.
 *
 * Everit - OSGi Reference Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Reference Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Reference Tracker.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.referencetracker;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ServiceReferenceTracker<S> extends AbstractReferenceTracker<ServiceReference<S>> {

    private class ReferenceTrackerCustomizer implements ServiceTrackerCustomizer<S, ServiceReference<S>> {

        @Override
        public ServiceReference<S> addingService(ServiceReference<S> reference) {
            addingReference(reference);
            return reference;
        }

        @Override
        public void modifiedService(ServiceReference<S> reference, ServiceReference<S> tracked) {
            modifiedReference(reference);
        }

        @Override
        public void removedService(ServiceReference<S> reference, ServiceReference<S> tracked) {
            removedReference(reference);
        }
    }

    private final boolean trackAllServices;

    private final ServiceTracker<S, ServiceReference<S>> tracker;

    public ServiceReferenceTracker(BundleContext context, Class<S> referenceType,
            ReferenceItem<ServiceReference<S>>[] items,
            boolean survivor, ReferenceActionHandler<ServiceReference<S>> actionHandler, boolean trackAllServices) {
        super(context, items, survivor, actionHandler);

        this.trackAllServices = trackAllServices;
        tracker = new ServiceTracker<S, ServiceReference<S>>(context, referenceType,
                new ReferenceTrackerCustomizer());
    }

    @Override
    protected void closeTracker() {
        tracker.close();
    }

    @Override
    protected ServiceReference<S>[] getAvailableReferences() {
        return tracker.getServiceReferences();
    }

    @Override
    protected boolean matches(ServiceReference<S> reference, Filter filter) {
        return filter == null || filter.match(reference);
    }

    @Override
    protected void openTracker() {
        tracker.open(trackAllServices);
    }
}
