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

import org.osgi.framework.Filter;

public class ReferenceItem<T> {

    private final String referenceItemId;

    private final Filter filter;

    private final Map<String, Object> attributes;

    public ReferenceItem(String itemId, Filter filter, Map<String, Object> attributes) {
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

}
