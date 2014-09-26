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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

public class BundleCapabilityTracker extends AbstractReferenceTracker<BundleCapability> {

    private class ReferenceTrackerCustomizer implements BundleTrackerCustomizer<Bundle> {

        @Override
        public Bundle addingBundle(Bundle bundle, BundleEvent event) {
            addingReference(null);
            return bundle;
        }

        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
            // TODO Auto-generated method stub

        }

    }

    private final BundleTracker<Bundle> tracker;

    public BundleCapabilityTracker(BundleContext context, ReferenceItem<BundleCapability>[] items, boolean survivor,
            ReferenceActionHandler<BundleCapability> actionHandler, int stateMask) {
        super(context, items, survivor, actionHandler);
        this.tracker = new BundleTracker<Bundle>(context, stateMask, new ReferenceTrackerCustomizer());
    }

    @Override
    protected void closeTracker() {
        tracker.close();
    }

    @Override
    protected BundleCapability[] getAvailableReferences() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean matches(BundleCapability reference, Filter filter) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void openTracker() {
        tracker.open();
    }

}
