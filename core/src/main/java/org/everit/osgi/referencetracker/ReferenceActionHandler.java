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

import org.osgi.framework.ServiceReference;

public interface ReferenceActionHandler<S> {

    void bind(ReferenceItem<S> referenceItem, ServiceReference<S> reference, S service, int position);

    void satisfied();

    void unsatisfied();

    void unbind(ReferenceItem<S> referenceItem);
}