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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

            if (survivor) {
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

                                callBindForItem(item, newReference);
                            } else {
                                boolean satisfied = isSatisfied();
                                unsatisfiedItems.add(item);
                                if (satisfied) {
                                    actionHandler.unsatisfied();
                                }

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
                                actionHandler.unsatisfied();
                            }

                            notMatchedItems.add(item);

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
                            callBindForItem(item, newReference);
                        }
                    }
                    unsatisfiedItems.addAll(notMatchedItems);
                    if (unsatisfied && isSatisfied()) {
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

            if (survivor) {
                Collection<ReferenceItem<S>> items = satisfiedReferenceItems.remove(reference);
                if (items != null) {
                    Iterator<ReferenceItem<S>> iterator = items.iterator();
                    while (iterator.hasNext()) {
                        ReferenceItem<S> item = iterator.next();
                        ServiceReference<S> newReference = searchNewReferenceForItem(item);
                        if (newReference != null) {
                            addItemToSatisfiedMap(item, newReference);

                            callBindForItem(item, newReference);
                        } else {
                            boolean satisfied = isSatisfied();

                            unsatisfiedItems.add(item);
                            if (satisfied) {
                                actionHandler.unsatisfied();
                            }

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
                        actionHandler.unsatisfied();
                    }
                    for (ReferenceItem<S> item : items) {
                        bundleContext.ungetService(reference);
                        actionHandler.unbind(item);
                    }

                    for (ReferenceItem<S> item : items) {
                        ServiceReference<S> newReference = searchNewReferenceForItem(item);

                        if (newReference != null) {
                            callBindForItem(item, newReference);

                            addItemToSatisfiedMap(item, newReference);
                            unsatisfiedItems.remove(item);
                        }
                    }

                    if (isSatisfied()) {
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
        if (!opened.get()) {
            return null;
        }

        ServiceReference<S>[] serviceReferences = tracker.getServiceReferences();

        if (serviceReferences == null) {
            return null;
        }

        ServiceReference<S> matchingReference = null;
        for (int i = 0, n = serviceReferences.length; i < n && matchingReference == null; i++) {
            Filter filter = item.getFilter();
            if (matches(serviceReferences[i], filter)) {
                matchingReference = serviceReferences[i];
            }
        }
        return matchingReference;
    }

    // TODO handle positions

    private final ServiceTracker<S, ServiceReference<S>> tracker;

    private Map<ReferenceItem<S>, Integer> positionsByItems;

    private final Set<ReferenceItem<S>> unsatisfiedItems = new HashSet<ReferenceItem<S>>();

    private final ReentrantReadWriteLock itemsRWLock = new ReentrantReadWriteLock(false);

    private final Map<ServiceReference<S>, Collection<ReferenceItem<S>>> satisfiedReferenceItems =
            new HashMap<ServiceReference<S>, Collection<ReferenceItem<S>>>();

    // TODO handle RuntimeException for every actionHandler call
    private final ReferenceActionHandler<S> actionHandler;

    private BundleContext bundleContext;

    private boolean survivor;

    public ReferenceTracker(BundleContext context, Class<S> referenceType, ReferenceItem<S>[] items,
            boolean survivor, ReferenceActionHandler<S> actionHandler) {

        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(actionHandler, "Action handler must not be null");
        Objects.requireNonNull(referenceType, "Reference type must not be null");
        Objects.requireNonNull(items, "Reference item array must not be null");

        this.bundleContext = context;
        this.actionHandler = actionHandler;
        this.survivor = survivor;
        this.unsatisfiedItems.addAll(Arrays.asList(items));

        validateItems(items);

        positionsByItems = generatePositionMap(items);

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
                throw new DuplicateReferenceItemIdException("The reference item id '" + item.getReferenceItemId()
                        + "' is duplicated");
            }
        }
    }

    private void callBindWithPosition(ReferenceItem<S> item, ServiceReference<S> serviceReference, S service) {
        Integer position = positionsByItems.get(item);
        actionHandler.bind(item, serviceReference, service, position);
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
                actionHandler.satisfied();
            }
        }

    }

    private void callBindForItem(ReferenceItem<S> unsatisfiedItem, ServiceReference<S> reference) {
        S service = bundleContext.getService(reference);
        callBindWithPosition(unsatisfiedItem, reference, service);

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

    private AtomicBoolean opened = new AtomicBoolean(false);

    public void close() {
        opened.set(false);
        if (noItems()) {
            actionHandler.unsatisfied();
        } else {
            tracker.close();
        }
    }

    public void open(boolean trackAllServices) {
        opened.set(true);
        if (noItems()) {
            actionHandler.satisfied();
        } else {
            tracker.open(trackAllServices);
        }
    }

    public void updateItems(ReferenceItem<S>[] items) {
        WriteLock writeLock = itemsRWLock.writeLock();
        writeLock.lock();

        Set<ReferenceItem<S>> newItemSet = new HashSet<ReferenceItem<S>>(Arrays.asList(items));
        Set<ReferenceItem<S>> existedItems = new HashSet<ReferenceItem<S>>();

        boolean satisfied = isSatisfied();

        removeNonExistentItemsFromUnsatisfiedCollection(newItemSet, existedItems);

        // Remove items from satisfied that are not contained in the new items

        satisfied = removeDeletedItemsFromSatisfied(newItemSet, satisfied, existedItems);

        // Handle position changes

        Map<ReferenceItem<S>, Integer> positionsByNewItems = generatePositionMap(items);

        satisfied = handlePositionChanges(existedItems, satisfied, positionsByNewItems);

        positionsByItems = positionsByNewItems;

        // TODO handle new items

        writeLock.unlock();
    }

    private static <S> Map<ReferenceItem<S>, Integer> generatePositionMap(ReferenceItem<S>[] items) {
        Map<ReferenceItem<S>, Integer> result = new HashMap<ReferenceItem<S>, Integer>();
        for (int i = 0; i < items.length; i++) {
            result.put(items[i], i);
        }

        return result;
    }

    private boolean handlePositionChanges(Set<ReferenceItem<S>> existedItems, boolean satisfied,
            Map<ReferenceItem<S>, Integer> positionsByNewItems) {
        if (satisfied && survivor) {
            // TODO
        } else {

        }
        return false;
    }

    private boolean removeDeletedItemsFromSatisfied(Set<ReferenceItem<S>> newItemSet, boolean satisfied,
            Set<ReferenceItem<S>> existedItems) {
        Iterator<Entry<ServiceReference<S>, Collection<ReferenceItem<S>>>> satisfiedEntryIterator =
                satisfiedReferenceItems.entrySet().iterator();

        while (satisfiedEntryIterator.hasNext()) {
            Entry<ServiceReference<S>, Collection<ReferenceItem<S>>> satisfiedEntry = satisfiedEntryIterator.next();
            Collection<ReferenceItem<S>> satisfiedItemCollection = satisfiedEntry.getValue();
            Iterator<ReferenceItem<S>> satisfiedItemIterator = satisfiedItemCollection.iterator();
            while (satisfiedItemIterator.hasNext()) {
                ReferenceItem<S> item = satisfiedItemIterator.next();
                if (!newItemSet.contains(item)) {
                    satisfiedItemIterator.remove();
                    if (satisfied && !survivor) {
                        actionHandler.unsatisfied();
                        satisfied = false;
                    }
                    actionHandler.unbind(item);
                } else {
                    existedItems.add(item);
                }
                if (satisfiedItemCollection.isEmpty()) {
                    bundleContext.ungetService(satisfiedEntry.getKey());
                    satisfiedEntryIterator.remove();
                }
            }
        }
        return satisfied;
    }

    private void removeNonExistentItemsFromUnsatisfiedCollection(Set<ReferenceItem<S>> itemList,
            Set<ReferenceItem<S>> existedItems) {
        Iterator<ReferenceItem<S>> iterator = unsatisfiedItems.iterator();

        while (iterator.hasNext()) {
            ReferenceItem<S> item = iterator.next();
            if (!itemList.contains(item)) {
                iterator.remove();
            } else {
                existedItems.add(item);
            }
        }
    }
}
