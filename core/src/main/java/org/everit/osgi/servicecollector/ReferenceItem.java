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
package org.everit.osgi.servicecollector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Filter;

public class ReferenceItem<S> {

    private final String referenceItemId;

    private final Filter filter;

    private final Map<String, Object> attributes;

    public ReferenceItem(String itemId, Filter filter, Map<String, Object> attributes) {
        Objects.requireNonNull(itemId, "Reference item id must be provided");
        Objects.requireNonNull(attributes, "Attributes for reference item must be provided");

        this.referenceItemId = itemId;
        this.filter = filter;
        this.attributes = new LinkedHashMap<String, Object>(attributes);
    }

    public String getReferenceItemId() {
        return referenceItemId;
    }

    public Filter getFilter() {
        return filter;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((referenceItemId == null) ? 0 : referenceItemId.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        @SuppressWarnings("unchecked")
        ReferenceItem<S> other = (ReferenceItem<S>) obj;
        if (referenceItemId == null) {
            if (other.referenceItemId != null)
                return false;
        } else if (!referenceItemId.equals(other.referenceItemId))
            return false;
        if (filter == null) {
            if (other.filter != null)
                return false;
        } else if (!filter.equals(other.filter))
            return false;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ReferenceItem [referenceItemId=" + referenceItemId + ", filter=" + filter + ", attributes="
                + attributes + "]";
    }

}
