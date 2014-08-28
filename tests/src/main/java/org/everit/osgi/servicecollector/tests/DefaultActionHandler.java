/**
 * This file is part of Everit - OSGi Service Collector Tests.
 *
 * Everit - OSGi Service Collector Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Service Collector Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Service Collector Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.servicecollector.tests;

import org.everit.osgi.servicecollector.ReferenceActionHandler;
import org.junit.Assert;
import org.osgi.framework.ServiceReference;

public final class DefaultActionHandler<S> implements ReferenceActionHandler<S> {

    private boolean satisfied = false;

    @Override
    public void bind(String referenceItemId, ServiceReference<S> reference, S service) {

    }

    @Override
    public void satisfied() {
        Assert.assertFalse(satisfied);
        satisfied = true;
    }

    @Override
    public void unsatisfied() {
        Assert.assertTrue(satisfied);
        satisfied = false;
    }

    @Override
    public void unbind(String referenceItemName) {
    }

    public boolean isSatisfied() {
        return satisfied;
    }

}
