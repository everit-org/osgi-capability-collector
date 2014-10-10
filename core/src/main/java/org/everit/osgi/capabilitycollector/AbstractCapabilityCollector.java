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

public abstract class AbstractCapabilityCollector<C> {

    // TODO handle RuntimeException for every actionHandler call
    private final ActionHandler<C> actionHandler;

    private final AtomicBoolean opened = new AtomicBoolean(false);

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);

    private final Map<C, Collection<RequirementDefinition<C>>> satisfiedRequirementsByCapabilities =
            new HashMap<C, Collection<RequirementDefinition<C>>>();

    private final boolean survivor;

    private final Set<RequirementDefinition<C>> unsatisfiedRequirements = new HashSet<RequirementDefinition<C>>();

    public AbstractCapabilityCollector(BundleContext context, RequirementDefinition<C>[] items,
            boolean survivor, ActionHandler<C> actionHandler) {

        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(actionHandler, "Action handler must not be null");
        Objects.requireNonNull(items, "Requirement item array must not be null");

        this.actionHandler = actionHandler;
        this.survivor = survivor;
        this.unsatisfiedRequirements.addAll(Arrays.asList(items));

        validateItems(items);
    }

    protected void addingCapablility(C capability) {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        tryCapabilityOnUnsatisfiedRequirements(capability);

        writeLock.unlock();
    }

    private void addRequirementToSatisfiedMap(RequirementDefinition<C> requirement, C capability) {
        Collection<RequirementDefinition<C>> requirements = satisfiedRequirementsByCapabilities.get(capability);
        if (requirements == null) {
            requirements = new ArrayList<RequirementDefinition<C>>();
            satisfiedRequirementsByCapabilities.put(capability, requirements);
        }
        requirements.add(requirement);
    }

    private void callBindForRequirement(RequirementDefinition<C> item, C capability) {
        actionHandler.bind(item, capability);
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

    protected abstract C[] getAvailableCapabilities();

    public boolean isSatisfied() {
        if (!opened.get()) {
            return false;
        }

        ReadLock readLock = readWriteLock.readLock();
        readLock.lock();
        boolean result = unsatisfiedRequirements.isEmpty();
        readLock.unlock();
        return result;
    }

    protected abstract boolean matches(C capability, Filter filter);

    protected void modifiedCapablility(C capability) {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        if (survivor) {
            Collection<RequirementDefinition<C>> requirements = satisfiedRequirementsByCapabilities.get(capability);
            if (requirements != null) {
                for (Iterator<RequirementDefinition<C>> iterator = requirements.iterator(); iterator.hasNext();) {
                    RequirementDefinition<C> requirement = iterator.next();
                    Filter filter = requirement.getFilter();
                    if (!matches(capability, filter)) {
                        iterator.remove();
                        C newMatchingCapability = searchMatchingCapabilityForRequirement(requirement);
                        if (newMatchingCapability != null) {
                            addRequirementToSatisfiedMap(requirement, newMatchingCapability);

                            callBindForRequirement(requirement, newMatchingCapability);
                        } else {
                            boolean satisfied = isSatisfied();
                            unsatisfiedRequirements.add(requirement);
                            if (satisfied) {
                                actionHandler.unsatisfied();
                            }

                            actionHandler.unbind(requirement);

                        }
                    }
                }
            }
        } else {
            Collection<RequirementDefinition<C>> requirements = satisfiedRequirementsByCapabilities.get(capability);
            if (requirements != null) {
                List<RequirementDefinition<C>> notMatchedRequirements = new LinkedList<RequirementDefinition<C>>();
                for (RequirementDefinition<C> requirement : requirements) {
                    Filter filter = requirement.getFilter();
                    if (!matches(capability, filter)) {
                        if (isSatisfied() && notMatchedRequirements.isEmpty()) {
                            actionHandler.unsatisfied();
                        }

                        notMatchedRequirements.add(requirement);

                        actionHandler.unbind(requirement);
                    }
                }

                boolean unsatisfied = !notMatchedRequirements.isEmpty();
                requirements.removeAll(notMatchedRequirements);
                if (requirements.isEmpty()) {
                    satisfiedRequirementsByCapabilities.remove(capability);
                }
                for (Iterator<RequirementDefinition<C>> iterator = notMatchedRequirements.iterator(); iterator
                        .hasNext();) {
                    RequirementDefinition<C> requirement = iterator.next();
                    C newCapability = searchMatchingCapabilityForRequirement(requirement);
                    if (newCapability != null) {
                        iterator.remove();
                        addRequirementToSatisfiedMap(requirement, newCapability);
                        callBindForRequirement(requirement, newCapability);
                    }
                }
                unsatisfiedRequirements.addAll(notMatchedRequirements);
                if (unsatisfied && isSatisfied()) {
                    actionHandler.satisfied();
                }
            }
        }
        tryCapabilityOnUnsatisfiedRequirements(capability);

        writeLock.unlock();
    }

    private boolean noItems() {
        boolean result;
        ReadLock readLock = readWriteLock.readLock();
        readLock.lock();

        result = satisfiedRequirementsByCapabilities.isEmpty() && unsatisfiedRequirements.isEmpty();

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

    protected void removedCapability(C capability) {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        if (survivor) {
            Collection<RequirementDefinition<C>> requirements = satisfiedRequirementsByCapabilities.remove(capability);
            if (requirements != null) {
                Iterator<RequirementDefinition<C>> iterator = requirements.iterator();
                while (iterator.hasNext()) {
                    RequirementDefinition<C> requirement = iterator.next();
                    C matchingCapability = searchMatchingCapabilityForRequirement(requirement);
                    if (matchingCapability != null) {
                        addRequirementToSatisfiedMap(requirement, matchingCapability);

                        callBindForRequirement(requirement, matchingCapability);
                    } else {
                        boolean satisfied = isSatisfied();

                        unsatisfiedRequirements.add(requirement);
                        if (satisfied) {
                            actionHandler.unsatisfied();
                        }

                        actionHandler.unbind(requirement);
                    }
                }
            }

        } else {
            Collection<RequirementDefinition<C>> requirements = satisfiedRequirementsByCapabilities.remove(capability);
            if (requirements != null) {
                boolean satisfied = isSatisfied();
                unsatisfiedRequirements.addAll(requirements);
                if (satisfied) {
                    actionHandler.unsatisfied();
                }
                for (RequirementDefinition<C> requirement : requirements) {
                    actionHandler.unbind(requirement);
                }

                for (RequirementDefinition<C> requirement : requirements) {
                    C matchingCapability = searchMatchingCapabilityForRequirement(requirement);

                    if (matchingCapability != null) {
                        callBindForRequirement(requirement, matchingCapability);
                        addRequirementToSatisfiedMap(requirement, matchingCapability);
                        unsatisfiedRequirements.remove(requirement);
                    }
                }

                if (isSatisfied()) {
                    actionHandler.satisfied();
                }
            }
        }

        writeLock.unlock();
    }

    private boolean removeDeletedRequirementsFromSatisfied(Set<RequirementDefinition<C>> newRequirementSet,
            boolean satisfied, Set<RequirementDefinition<C>> existedNewRequirements) {
        Iterator<Entry<C, Collection<RequirementDefinition<C>>>> satisfiedEntryIterator =
                satisfiedRequirementsByCapabilities.entrySet().iterator();

        while (satisfiedEntryIterator.hasNext()) {
            Entry<C, Collection<RequirementDefinition<C>>> satisfiedEntry = satisfiedEntryIterator.next();
            Collection<RequirementDefinition<C>> satisfiedRequirementCollection = satisfiedEntry.getValue();
            Iterator<RequirementDefinition<C>> satisfiedRequirementIterator = satisfiedRequirementCollection.iterator();
            while (satisfiedRequirementIterator.hasNext()) {
                RequirementDefinition<C> requirement = satisfiedRequirementIterator.next();
                if (!newRequirementSet.contains(requirement)) {
                    satisfiedRequirementIterator.remove();
                    if (satisfied && !survivor) {
                        actionHandler.unsatisfied();
                        satisfied = false;
                    }
                    actionHandler.unbind(requirement);
                } else {
                    existedNewRequirements.add(requirement);
                }
            }
            if (satisfiedRequirementCollection.isEmpty()) {
                satisfiedEntryIterator.remove();
            }
        }
        return satisfied;
    }

    private void removeNonExistentRequirementsFromUnsatisfiedCollection(Set<RequirementDefinition<C>> requirementList,
            Set<RequirementDefinition<C>> existedNewRequirements) {
        Iterator<RequirementDefinition<C>> iterator = unsatisfiedRequirements.iterator();

        while (iterator.hasNext()) {
            RequirementDefinition<C> requirement = iterator.next();
            if (!requirementList.contains(requirement)) {
                iterator.remove();
            } else {
                existedNewRequirements.add(requirement);
            }
        }
    }

    private C searchMatchingCapabilityForRequirement(RequirementDefinition<C> requirement) {
        if (!opened.get()) {
            return null;
        }

        C[] availableCapabilities = getAvailableCapabilities();

        if (availableCapabilities == null) {
            return null;
        }

        C matchingCapability = null;
        for (int i = 0, n = availableCapabilities.length; i < n && matchingCapability == null; i++) {
            Filter filter = requirement.getFilter();
            if (matches(availableCapabilities[i], filter)) {
                matchingCapability = availableCapabilities[i];
            }
        }
        return matchingCapability;
    }

    private void tryCapabilityOnUnsatisfiedRequirements(C capability) {
        if (!isSatisfied()) {
            for (Iterator<RequirementDefinition<C>> iterator = unsatisfiedRequirements.iterator(); iterator.hasNext();) {
                RequirementDefinition<C> requirement = iterator.next();
                Filter filter = requirement.getFilter();
                if (matches(capability, filter)) {
                    callBindForRequirement(requirement, capability);
                    iterator.remove();
                    addRequirementToSatisfiedMap(requirement, capability);
                }
            }
            if (isSatisfied()) {
                actionHandler.satisfied();
            }
        }

    }

    /**
     * Updates the requirements of this collector. Those requirements that were already satisfied remain unchanged.
     *
     * @param requirements
     *            The list of reference items to be tracked.
     * @throws NullPointerException
     *             if any of the element of the item array is null.
     * @throws DuplicateRequirementIdException
     *             if two or more reference items contain the same id.
     */
    public void updateItems(RequirementDefinition<C>[] requirements) {
        Objects.requireNonNull(requirements, "Items cannot be null");
        validateItems(requirements);

        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        boolean thereWereRequirements = !noItems();

        if (opened.get() && requirements.length > 0 && !thereWereRequirements) {
            openTracker();
        }

        Set<RequirementDefinition<C>> newRequirements = new HashSet<RequirementDefinition<C>>(
                Arrays.asList(requirements));
        Set<RequirementDefinition<C>> existedNewRequirements = new HashSet<RequirementDefinition<C>>();

        boolean satisfied = isSatisfied();

        removeNonExistentRequirementsFromUnsatisfiedCollection(newRequirements, existedNewRequirements);

        satisfied = removeDeletedRequirementsFromSatisfied(newRequirements, satisfied, existedNewRequirements);

        for (RequirementDefinition<C> newRequirement : newRequirements) {
            if (!existedNewRequirements.contains(newRequirement)) {
                C matchingCapability = searchMatchingCapabilityForRequirement(newRequirement);
                if (matchingCapability == null) {
                    if (satisfied) {
                        actionHandler.unsatisfied();
                        satisfied = false;
                    }
                    unsatisfiedRequirements.add(newRequirement);
                } else {
                    addRequirementToSatisfiedMap(newRequirement, matchingCapability);
                    callBindForRequirement(newRequirement, matchingCapability);
                }
            }
        }

        if (!satisfied && isSatisfied()) {
            actionHandler.satisfied();
        }

        if (opened.get() && thereWereRequirements && noItems()) {
            closeTracker();
        }

        writeLock.unlock();
    }

    private void validateItems(RequirementDefinition<C>[] requirements) {
        Set<String> usedIds = new HashSet<String>(requirements.length);

        for (RequirementDefinition<C> requirement : requirements) {
            if (requirement == null) {
                throw new NullPointerException("Null item in reference items.");
            }
            boolean newId = usedIds.add(requirement.getRequirementId());
            if (!newId) {
                throw new DuplicateRequirementIdException("The requirement id '" + requirement.getRequirementId()
                        + "' is duplicated in the collector");
            }
        }
    }
}
