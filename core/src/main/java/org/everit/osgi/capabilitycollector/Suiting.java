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

public class Suiting<C> {

    private final C capability;

    private final RequirementDefinition<C> requirement;

    public Suiting(final RequirementDefinition<C> requirement, final C capability) {
        this.requirement = requirement;
        this.capability = capability;
    }

    public C getCapability() {
        return capability;
    }

    public RequirementDefinition<C> getRequirement() {
        return requirement;
    }

}
