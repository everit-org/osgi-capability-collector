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

import java.util.LinkedList;

import org.everit.osgi.capabilitycollector.CapabilityConsumer;
import org.everit.osgi.capabilitycollector.Suiting;

public class TestCapabilityConsumer<C> implements CapabilityConsumer<C> {

    private final LinkedList<CallParameters<C>> callHistory = new LinkedList<CallParameters<C>>();

    private boolean satisfied = false;

    @Override
    public void accept(Suiting<C>[] suitings, Boolean satisfied) {
        callHistory.add(new CallParameters<C>(satisfied, suitings));
        this.satisfied = satisfied;
    }

    public void clearHistory() {
        callHistory.clear();
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public CallParameters<C> pollCallParameters() {
        if (callHistory.isEmpty()) {
            return null;
        }
        return callHistory.removeFirst();
    }

}
