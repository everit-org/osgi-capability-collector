/**
 * This file is part of Everit - OSGi Reference Tracker Tests.
 *
 * Everit - OSGi Reference Tracker Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Reference Tracker Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Reference Tracker Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.servicecollector.tests;

public class StringHolder {

    private final String value;

    public StringHolder() {
        this(null);
    }

    public StringHolder(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
