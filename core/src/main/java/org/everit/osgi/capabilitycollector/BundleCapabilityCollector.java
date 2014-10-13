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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class BundleCapabilityCollector extends AbstractCapabilityCollector<BundleCapability> {

    private class TrackerCustomizer implements BundleTrackerCustomizer<Bundle> {

        @Override
        public Bundle addingBundle(Bundle bundle, BundleEvent event) {
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            List<BundleCapability> capabilities = bundleWiring.getCapabilities(namespace);
            for (BundleCapability bundleCapability : capabilities) {
                addingCapablility(bundleCapability);
            }

            Lock writeLock = readWriteLock.writeLock();
            writeLock.lock();
            availableCapabilities.addAll(capabilities);
            writeLock.unlock();

            return bundle;
        }

        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
            // Do nothing as this is only about bundle state change.
        }

        @Override
        public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            List<BundleCapability> capabilities = bundleWiring.getCapabilities(namespace);

            Lock writeLock = readWriteLock.writeLock();
            writeLock.lock();
            availableCapabilities.removeAll(capabilities);
            writeLock.unlock();

            for (BundleCapability bundleCapability : capabilities) {
                removedCapability(bundleCapability);
            }
        }

    }

    private final Set<BundleCapability> availableCapabilities = new HashSet<BundleCapability>();

    private final String namespace;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final BundleTracker<Bundle> tracker;

    public BundleCapabilityCollector(BundleContext context, String namespace,
            RequirementDefinition<BundleCapability>[] requirements,
            CapabilityConsumer<BundleCapability> capabilityConsumer, int stateMask) {
        super(context, requirements, capabilityConsumer);

        if (((~(Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE)) & stateMask) > 0) {
            // TODO Custom exception type
            throw new RuntimeException(
                    "Only RESOLVED, STARTING, ACTIVE and STOPPING states are allowed in the bundle stateMask: "
                            + stateMask);
        }

        this.namespace = namespace;
        this.tracker = new BundleTracker<Bundle>(context, stateMask, new TrackerCustomizer());
    }

    @Override
    protected void closeTracker() {
        tracker.close();
    }

    @Override
    protected BundleCapability[] getAvailableCapabilities() {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        BundleCapability[] result = availableCapabilities.toArray(new BundleCapability[availableCapabilities.size()]);
        readLock.unlock();
        return result;
    }

    @Override
    protected boolean matches(BundleCapability capability, Filter filter) {
        Map<String, Object> attributes = capability.getAttributes();
        return filter.matches(attributes);
    }

    @Override
    protected void openTracker() {
        tracker.open();
    }

}
