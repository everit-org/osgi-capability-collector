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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ReferenceTracker<S> {

    private class ReferenceTrackerCustomizer implements ServiceTrackerCustomizer<S, ServiceReference<S>> {

        @Override
        public ServiceReference<S> addingService(ServiceReference<S> reference) {
            WriteLock writeLock = itemsRWLock.writeLock();
            writeLock.lock();

            tryServiceOnUnsatisfiedItems(reference);

            writeLock.unlock();
            return reference;
        }

        @Override
        public void modifiedService(ServiceReference<S> reference, ServiceReference<S> tracked) {
            WriteLock writeLock = itemsRWLock.writeLock();
            writeLock.lock();

            if (rebindOnReplace) {
                Collection<ReferenceItem<S>> items = satisfiedReferenceItems.get(reference);
                if (items != null) {
                    for (Iterator<ReferenceItem<S>> iterator = items.iterator(); iterator.hasNext();) {
                        ReferenceItem<S> item = iterator.next();
                        Filter filter = item.getFilter();
                        if (!matches(reference, filter)) {
                            iterator.remove();
                            ServiceReference<S> newReference = searchNewReferenceForItem(item);
                            if (newReference != null) {
                                addItemToSatisfiedMap(item, newReference);
                                S service = bundleContext.getService(newReference);

                                // TODO handle exceptions
                                actionHandler.bind(item, newReference, service);
                            } else {
                                boolean satisfied = isSatisfied();
                                unsatisfiedItems.add(item);
                                if (satisfied) {
                                    // TODO handle exceptions
                                    actionHandler.unsatisfied();
                                }

                                // TODO handle exceptions
                                actionHandler.unbind(item);

                            }
                            bundleContext.ungetService(reference);
                        }
                    }
                }
            } else {
                Collection<ReferenceItem<S>> items = satisfiedReferenceItems.get(reference);
                if (items != null) {
                    List<ReferenceItem<S>> notMatchedItems = new LinkedList<ReferenceItem<S>>();
                    for (Iterator<ReferenceItem<S>> iterator = items.iterator(); iterator.hasNext();) {
                        ReferenceItem<S> item = iterator.next();
                        Filter filter = item.getFilter();
                        if (!matches(reference, filter)) {
                            if (isSatisfied() && notMatchedItems.isEmpty()) {
                                // TODO handle exceptions
                                actionHandler.unsatisfied();
                            }

                            notMatchedItems.add(item);

                            // TODO handle exceptions
                            actionHandler.unbind(item);
                            bundleContext.ungetService(reference);
                        }
                    }

                    boolean unsatisfied = !notMatchedItems.isEmpty();
                    items.removeAll(notMatchedItems);
                    if (items.isEmpty()) {
                        satisfiedReferenceItems.remove(reference);
                    }
                    for (Iterator<ReferenceItem<S>> iterator = notMatchedItems.iterator(); iterator.hasNext();) {
                        ReferenceItem<S> item = iterator.next();
                        ServiceReference<S> newReference = searchNewReferenceForItem(item);
                        if (newReference != null) {
                            iterator.remove();
                            addItemToSatisfiedMap(item, newReference);
                            // TODO handle exceptions
                            S service = bundleContext.getService(newReference);
                            actionHandler.bind(item, newReference, service);
                        }
                    }
                    unsatisfiedItems.addAll(notMatchedItems);
                    if (unsatisfied && isSatisfied()) {
                        // TODO handle exceptions
                        actionHandler.satisfied();
                    }
                }
            }
            tryServiceOnUnsatisfiedItems(reference);

            writeLock.unlock();
        }

        @Override
        public void removedService(ServiceReference<S> reference, ServiceReference<S> tracked) {
            WriteLock writeLock = itemsRWLock.writeLock();
            writeLock.lock();

            if (rebindOnReplace) {
                Collection<ReferenceItem<S>> items = satisfiedReferenceItems.get(reference);
                if (items != null) {
                    Iterator<ReferenceItem<S>> iterator = items.iterator();
                    while (iterator.hasNext()) {
                        ReferenceItem<S> item = iterator.next();
                        ServiceReference<S> newReference = searchNewReferenceForItem(item);
                        if (newReference != null) {
                            S newService = bundleContext.getService(newReference);

                            // TODO handle exceptions
                            actionHandler.bind(item, newReference, newService);
                        } else {
                            boolean satisfied = isSatisfied();
                            iterator.remove();
                            unsatisfiedItems.add(item);
                            if (satisfied) {
                                // TODO handle exceptions
                                actionHandler.unsatisfied();
                            }

                            // TODO handle exceptions
                            actionHandler.unbind(item);
                        }
                        bundleContext.ungetService(reference);
                    }
                }

            } else {
                Collection<ReferenceItem<S>> items = satisfiedReferenceItems.remove(reference);
                if (items != null) {
                    boolean satisfied = isSatisfied();
                    unsatisfiedItems.addAll(items);
                    if (satisfied) {
                        // TODO handle exceptions
                        actionHandler.unsatisfied();
                    }
                    for (ReferenceItem<S> item : items) {
                        bundleContext.ungetService(reference);
                        // TODO handle exceptions
                        actionHandler.unbind(item);
                    }

                    for (ReferenceItem<S> item : items) {
                        ServiceReference<S> newReference = searchNewReferenceForItem(item);

                        if (newReference != null) {
                            // TODO handle exceptions
                            actionHandler.bind(item, newReference,
                                    bundleContext.getService(newReference));

                            addItemToSatisfiedMap(item, newReference);
                            unsatisfiedItems.remove(item);
                        }
                    }

                    if (isSatisfied()) {
                        // TODO handle exceptions
                        actionHandler.satisfied();
                    }
                }
            }

            writeLock.unlock();
        }
    }

    private boolean matches(ServiceReference<S> reference, Filter filter) {
        return filter == null || filter.match(reference);
    }

    private ServiceReference<S> searchNewReferenceForItem(ReferenceItem<S> item) {
        ServiceReference<S>[] serviceReferences = tracker.getServiceReferences();
        ServiceReference<S> matchingReference = null;
        for (int i = 0, n = serviceReferences.length; i < n && matchingReference == null; i++) {
            Filter filter = item.getFilter();
            if (matches(serviceReferences[i], filter)) {
                matchingReference = serviceReferences[i];
            }
        }
        return matchingReference;
    }

    private final ServiceTracker<S, ServiceReference<S>> tracker;

    private final Set<ReferenceItem<S>> unsatisfiedItems = new HashSet<ReferenceItem<S>>();

    private final ReentrantReadWriteLock itemsRWLock = new ReentrantReadWriteLock(false);

    private final Map<ServiceReference<S>, Collection<ReferenceItem<S>>> satisfiedReferenceItems =
            new HashMap<ServiceReference<S>, Collection<ReferenceItem<S>>>();

    private final ReferenceActionHandler<S> actionHandler;

    private BundleContext bundleContext;

    private boolean rebindOnReplace;

    public ReferenceTracker(BundleContext context, Class<S> referenceType, ReferenceItem<S>[] items,
            boolean rebindOnReplace, ReferenceActionHandler<S> actionHandler) {

        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(actionHandler, "Action handler must not be null");
        Objects.requireNonNull(referenceType, "Reference type must not be null");
        Objects.requireNonNull(items, "Reference item array must not be null");

        this.bundleContext = context;
        this.actionHandler = actionHandler;
        this.rebindOnReplace = rebindOnReplace;
        this.unsatisfiedItems.addAll(Arrays.asList(items));

        validateItems(items);

        if (noItems()) {
            tracker = null;
            return;
        }

        tracker = new ServiceTracker<S, ServiceReference<S>>(context, referenceType,
                new ReferenceTrackerCustomizer());
    }

    private void validateItems(ReferenceItem<S>[] items) {
        Set<String> usedIds = new HashSet<String>(items.length);

        for (ReferenceItem<S> item : items) {
            if (item == null) {
                throw new NullPointerException("Null item in reference items.");
            }
            boolean newId = usedIds.add(item.getReferenceItemId());
            if (!newId) {
                throw new DuplicateReferenceIdException("The reference item id '" + item.getReferenceItemId()
                        + "' is duplicated");
            }
        }
    }

    private void tryServiceOnUnsatisfiedItems(ServiceReference<S> reference) {
        if (!isSatisfied()) {
            for (Iterator<ReferenceItem<S>> iterator = unsatisfiedItems.iterator(); iterator.hasNext();) {
                ReferenceItem<S> unsatisfiedItem = iterator.next();
                Filter filter = unsatisfiedItem.getFilter();
                if (matches(reference, filter)) {
                    callBindForItem(unsatisfiedItem, reference);
                    iterator.remove();
                    addItemToSatisfiedMap(unsatisfiedItem, reference);
                }
            }
            if (isSatisfied()) {
                try {
                    actionHandler.satisfied();
                } catch (RuntimeException e) {
                    // TODO
                }
            }
        }

    }

    private void callBindForItem(ReferenceItem<S> unsatisfiedItem, ServiceReference<S> reference) {
        S service = null;
        try {
            service = bundleContext.getService(reference);
            actionHandler.bind(unsatisfiedItem, reference, service);
        } catch (RuntimeException e) {
            if (service != null) {
                bundleContext.ungetService(reference);
            }
            // TODO
            return;
        }

    }

    private void addItemToSatisfiedMap(ReferenceItem<S> unsatisfiedItem, ServiceReference<S> reference) {
        Collection<ReferenceItem<S>> items = satisfiedReferenceItems.get(reference);
        if (items == null) {
            items = new ArrayList<ReferenceItem<S>>();
            satisfiedReferenceItems.put(reference, items);
        }
        items.add(unsatisfiedItem);
    }

    public boolean isSatisfied() {
        ReadLock readLock = itemsRWLock.readLock();
        readLock.lock();
        boolean result = unsatisfiedItems.isEmpty();
        readLock.unlock();
        return result;
    }

    private boolean noItems() {
        boolean result;
        ReadLock readLock = itemsRWLock.readLock();
        readLock.lock();

        result = satisfiedReferenceItems.isEmpty() && unsatisfiedItems.isEmpty();

        readLock.unlock();
        return result;
    }

    public void close() {
        if (noItems()) {
            actionHandler.unsatisfied();
        } else {
            tracker.close();
        }
    }

    public void open(boolean trackAllServices) {
        if (noItems()) {
            actionHandler.satisfied();
        } else {
            tracker.open(trackAllServices);
        }
    }
}
