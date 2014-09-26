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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Filter;

/**
 *
 * @param <R>
 *            Type of reference instances.
 */
public class ReferenceItem<R> {

    private final Map<String, Object> attributes;

    private final Filter filter;

    private final String referenceItemId;

    public ReferenceItem(String itemId, Filter filter, Map<String, Object> attributes) {
        Objects.requireNonNull(itemId, "Reference item id must be provided");
        Objects.requireNonNull(attributes, "Attributes for reference item must be provided");

        this.referenceItemId = itemId;
        this.filter = filter;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(attributes));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        ReferenceItem<R> other = (ReferenceItem<R>) obj;
        if (referenceItemId == null) {
            if (other.referenceItemId != null) {
                return false;
            }
        } else if (!referenceItemId.equals(other.referenceItemId)) {
            return false;
        }
        if (filter == null) {
            if (other.filter != null) {
                return false;
            }
        } else if (!filter.equals(other.filter)) {
            return false;
        }
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        return true;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Filter getFilter() {
        return filter;
    }

    public String getReferenceItemId() {
        return referenceItemId;
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
    public String toString() {
        return "ReferenceItem [referenceItemId=" + referenceItemId + ", filter=" + filter + ", attributes="
                + attributes + "]";
    }

}
