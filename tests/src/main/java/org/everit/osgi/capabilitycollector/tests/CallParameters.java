/**
 * This file is part of Everit - OSGi Capability Collector Tests.
 *
 * Everit - OSGi Capability Collector Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Capability Collector Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Capability Collector Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.capabilitycollector.tests;

import org.everit.osgi.capabilitycollector.Suiting;

public class CallParameters<C> {

    private final boolean satisfied;

    private final Suiting<C>[] suitings;

    public CallParameters(boolean satisfied, Suiting<C>[] suitings) {
        this.satisfied = satisfied;
        this.suitings = suitings;
    }

    public Suiting<C>[] getSuitings() {
        return suitings;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

}
