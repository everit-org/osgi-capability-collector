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

public abstract class AbstractReferenceTracker<R> {

    // TODO handle RuntimeException for every actionHandler call
    private final ReferenceActionHandler<R> actionHandler;

    private final ReentrantReadWriteLock itemsRWLock = new ReentrantReadWriteLock(false);

    private final AtomicBoolean opened = new AtomicBoolean(false);

    private final Map<R, Collection<ReferenceItem<R>>> satisfiedReferenceItems =
            new HashMap<R, Collection<ReferenceItem<R>>>();

    private final boolean survivor;

    private final Set<ReferenceItem<R>> unsatisfiedItems = new HashSet<ReferenceItem<R>>();

    public AbstractReferenceTracker(BundleContext context, ReferenceItem<R>[] items,
            boolean survivor, ReferenceActionHandler<R> actionHandler) {

        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(actionHandler, "Action handler must not be null");
        Objects.requireNonNull(items, "Reference item array must not be null");

        this.actionHandler = actionHandler;
        this.survivor = survivor;
        this.unsatisfiedItems.addAll(Arrays.asList(items));

        validateItems(items);
    }

    protected void addingReference(R reference) {
        WriteLock writeLock = itemsRWLock.writeLock();
        writeLock.lock();

        tryReferenceOnUnsatisfiedItems(reference);

        writeLock.unlock();
    }

    private void addItemToSatisfiedMap(ReferenceItem<R> unsatisfiedItem, R reference) {
        Collection<ReferenceItem<R>> items = satisfiedReferenceItems.get(reference);
        if (items == null) {
            items = new ArrayList<ReferenceItem<R>>();
            satisfiedReferenceItems.put(reference, items);
        }
        items.add(unsatisfiedItem);
    }

    private void callBindForItem(ReferenceItem<R> item, R reference) {
        actionHandler.bind(item, reference);
    }

    public void close() {
        opened.set(false);
        if (noItems()) {
            actionHandler.unsatisfied();
        } else {
            closeTracker();
        }
    }

    protected abstract void closeTracker();

    protected abstract R[] getAvailableReferences();

    public boolean isSatisfied() {
        if (!opened.get()) {
            return false;
        }

        ReadLock readLock = itemsRWLock.readLock();
        readLock.lock();
        boolean result = unsatisfiedItems.isEmpty();
        readLock.unlock();
        return result;
    }

    protected abstract boolean matches(R reference, Filter filter);

    protected void modifiedReference(R reference) {
        WriteLock writeLock = itemsRWLock.writeLock();
        writeLock.lock();

        if (survivor) {
            Collection<ReferenceItem<R>> items = satisfiedReferenceItems.get(reference);
            if (items != null) {
                for (Iterator<ReferenceItem<R>> iterator = items.iterator(); iterator.hasNext();) {
                    ReferenceItem<R> item = iterator.next();
                    Filter filter = item.getFilter();
                    if (!matches(reference, filter)) {
                        iterator.remove();
                        R newReference = searchNewReferenceForItem(item);
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
                    }
                }
            }
        } else {
            Collection<ReferenceItem<R>> items = satisfiedReferenceItems.get(reference);
            if (items != null) {
                List<ReferenceItem<R>> notMatchedItems = new LinkedList<ReferenceItem<R>>();
                for (ReferenceItem<R> item : items) {
                    Filter filter = item.getFilter();
                    if (!matches(reference, filter)) {
                        if (isSatisfied() && notMatchedItems.isEmpty()) {
                            actionHandler.unsatisfied();
                        }

                        notMatchedItems.add(item);

                        actionHandler.unbind(item);
                    }
                }

                boolean unsatisfied = !notMatchedItems.isEmpty();
                items.removeAll(notMatchedItems);
                if (items.isEmpty()) {
                    satisfiedReferenceItems.remove(reference);
                }
                for (Iterator<ReferenceItem<R>> iterator = notMatchedItems.iterator(); iterator.hasNext();) {
                    ReferenceItem<R> item = iterator.next();
                    R newReference = searchNewReferenceForItem(item);
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
        tryReferenceOnUnsatisfiedItems(reference);

        writeLock.unlock();
    }

    private boolean noItems() {
        boolean result;
        ReadLock readLock = itemsRWLock.readLock();
        readLock.lock();

        result = satisfiedReferenceItems.isEmpty() && unsatisfiedItems.isEmpty();

        readLock.unlock();
        return result;
    }

    public void open() {
        opened.set(true);
        if (noItems()) {
            actionHandler.satisfied();
        } else {
            openTracker();
        }
    }

    protected abstract void openTracker();

    private boolean removeDeletedItemsFromSatisfied(Set<ReferenceItem<R>> newItemSet, boolean satisfied,
            Set<ReferenceItem<R>> existedItems) {
        Iterator<Entry<R, Collection<ReferenceItem<R>>>> satisfiedEntryIterator =
                satisfiedReferenceItems.entrySet().iterator();

        while (satisfiedEntryIterator.hasNext()) {
            Entry<R, Collection<ReferenceItem<R>>> satisfiedEntry = satisfiedEntryIterator.next();
            Collection<ReferenceItem<R>> satisfiedItemCollection = satisfiedEntry.getValue();
            Iterator<ReferenceItem<R>> satisfiedItemIterator = satisfiedItemCollection.iterator();
            while (satisfiedItemIterator.hasNext()) {
                ReferenceItem<R> item = satisfiedItemIterator.next();
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
            }
            if (satisfiedItemCollection.isEmpty()) {
                satisfiedEntryIterator.remove();
            }
        }
        return satisfied;
    }

    protected void removedReference(R reference) {
        WriteLock writeLock = itemsRWLock.writeLock();
        writeLock.lock();

        if (survivor) {
            Collection<ReferenceItem<R>> items = satisfiedReferenceItems.remove(reference);
            if (items != null) {
                Iterator<ReferenceItem<R>> iterator = items.iterator();
                while (iterator.hasNext()) {
                    ReferenceItem<R> item = iterator.next();
                    R newReference = searchNewReferenceForItem(item);
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
                }
            }

        } else {
            Collection<ReferenceItem<R>> items = satisfiedReferenceItems.remove(reference);
            if (items != null) {
                boolean satisfied = isSatisfied();
                unsatisfiedItems.addAll(items);
                if (satisfied) {
                    actionHandler.unsatisfied();
                }
                for (ReferenceItem<R> item : items) {
                    actionHandler.unbind(item);
                }

                for (ReferenceItem<R> item : items) {
                    R newReference = searchNewReferenceForItem(item);

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

    private void removeNonExistentItemsFromUnsatisfiedCollection(Set<ReferenceItem<R>> itemList,
            Set<ReferenceItem<R>> existedItems) {
        Iterator<ReferenceItem<R>> iterator = unsatisfiedItems.iterator();

        while (iterator.hasNext()) {
            ReferenceItem<R> item = iterator.next();
            if (!itemList.contains(item)) {
                iterator.remove();
            } else {
                existedItems.add(item);
            }
        }
    }

    private R searchNewReferenceForItem(ReferenceItem<R> item) {
        if (!opened.get()) {
            return null;
        }

        R[] availableReferences = getAvailableReferences();

        if (availableReferences == null) {
            return null;
        }

        R matchingReference = null;
        for (int i = 0, n = availableReferences.length; i < n && matchingReference == null; i++) {
            Filter filter = item.getFilter();
            if (matches(availableReferences[i], filter)) {
                matchingReference = availableReferences[i];
            }
        }
        return matchingReference;
    }

    private void tryReferenceOnUnsatisfiedItems(R reference) {
        if (!isSatisfied()) {
            for (Iterator<ReferenceItem<R>> iterator = unsatisfiedItems.iterator(); iterator.hasNext();) {
                ReferenceItem<R> unsatisfiedItem = iterator.next();
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

    /**
     * Updates the items of this tracker. Those items that were already satisfied remain unchanged.
     *
     * @param items
     *            The list of reference items to be tracked.
     * @throws NullPointerException
     *             if any of the element of the item array is null.
     * @throws DuplicateReferenceItemIdException
     *             if two or more reference items contain the same id.
     */
    public void updateItems(ReferenceItem<R>[] items) {
        Objects.requireNonNull(items, "Items cannot be null");
        validateItems(items);

        WriteLock writeLock = itemsRWLock.writeLock();
        writeLock.lock();

        boolean thereWereItems = !noItems();

        if (opened.get() && items.length > 0 && !thereWereItems) {
            openTracker();
        }

        Set<ReferenceItem<R>> newItemSet = new HashSet<ReferenceItem<R>>(Arrays.asList(items));
        Set<ReferenceItem<R>> existedItems = new HashSet<ReferenceItem<R>>();

        boolean satisfied = isSatisfied();

        removeNonExistentItemsFromUnsatisfiedCollection(newItemSet, existedItems);

        satisfied = removeDeletedItemsFromSatisfied(newItemSet, satisfied, existedItems);

        if (!survivor && satisfied && !existedItems.equals(newItemSet)) {
            actionHandler.unsatisfied();
            satisfied = false;
        }

        for (ReferenceItem<R> newItem : newItemSet) {
            if (!existedItems.contains(newItem)) {
                R referenceForNewItem = searchNewReferenceForItem(newItem);
                if (referenceForNewItem == null) {
                    if (satisfied) {
                        actionHandler.unsatisfied();
                        satisfied = false;
                    }
                    unsatisfiedItems.add(newItem);
                } else {
                    addItemToSatisfiedMap(newItem, referenceForNewItem);
                    callBindForItem(newItem, referenceForNewItem);
                }
            }
        }

        if (!satisfied && isSatisfied()) {
            actionHandler.satisfied();
        }

        if (opened.get() && thereWereItems && noItems()) {
            closeTracker();
        }

        writeLock.unlock();
    }

    private void validateItems(ReferenceItem<R>[] items) {
        Set<String> usedIds = new HashSet<String>(items.length);

        for (ReferenceItem<R> item : items) {
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
}
