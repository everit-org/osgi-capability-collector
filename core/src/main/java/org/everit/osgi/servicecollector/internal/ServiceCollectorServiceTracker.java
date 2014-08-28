/**
 * This file is part of Everit - OSGi Service Collector.
 *
 * Everit - OSGi Service Collector is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Service Collector is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Service Collector.  If not, see <http://www.gnu.org/licenses/>.
 */
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
