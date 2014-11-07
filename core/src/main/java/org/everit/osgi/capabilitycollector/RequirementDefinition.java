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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Filter;

/**
 *
 * @param <C>
 *            Type of capability that satisfies this requirement.
 */
public class RequirementDefinition<C> {

    private final Map<String, Object> attributes;

    private final Filter filter;

    private final String requirementId;

    public RequirementDefinition(final String requirementId, final Filter filter, final Map<String, Object> attributes) {
        Objects.requireNonNull(requirementId, "Requirement id must be provided");
        Objects.requireNonNull(attributes,
                "Attributes for requirement must be provided at least with a zero element map");

        this.requirementId = requirementId;
        this.filter = filter;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(attributes));
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Filter getFilter() {
        return filter;
    }

    public String getRequirementId() {
        return requirementId;
    }

    @Override
    public String toString() {
        return "RequirementDefinition [requirementId=" + requirementId + ", filter=" + filter + ", attributes="
                + attributes + "]";
    }

}
