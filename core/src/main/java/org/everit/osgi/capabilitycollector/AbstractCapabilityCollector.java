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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

public abstract class AbstractCapabilityCollector<C> {

    /**
     * TODO Handle exceptions coming out from consumer
     */
    private final CapabilityConsumer<C> capabilityConsumer;

    private volatile boolean opened = false;

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);

    private volatile boolean satisfied = false;

    private Suiting<C>[] suitings;

    public AbstractCapabilityCollector(final BundleContext context, final RequirementDefinition<C>[] requirements,
            final CapabilityConsumer<C> capabilityConsumer) {

        Objects.requireNonNull(context, "Context must not be null");
        Objects.requireNonNull(capabilityConsumer, "Capability consumer must not be null");
        Objects.requireNonNull(requirements, "Requirement item array must not be null");

        this.capabilityConsumer = capabilityConsumer;

        validateRequirements(requirements);

        Suiting<C>[] lSuitings = createSuitingsWithoutCapability(requirements);
        this.suitings = lSuitings;
    }

    protected void addingCapablility(final C capability) {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        boolean changed = tryCapabilityOnUnsatisfiedRequirements(capability);
        if (changed) {
            capabilityConsumer.accept(suitings, satisfied);
        }

        writeLock.unlock();
    }

    private boolean areNewRequirementsSame(final RequirementDefinition<C>[] newRequirements) {
        if (newRequirements.length != suitings.length) {
            return false;
        }

        for (int i = 0, n = suitings.length; i < n; i++) {
            RequirementDefinition<C> oldRequirement = suitings[i].getRequirement();
            RequirementDefinition<C> newRequirement = newRequirements[i];
            Filter oldFilter = oldRequirement.getFilter();
            Filter newFilter = newRequirement.getFilter();
            if (oldRequirement != newRequirement
                    || !oldRequirement.getRequirementId().equals(newRequirement.getRequirementId())
                    || !Objects.equals(oldFilter, newFilter)
                    || !oldRequirement.getAttributes().equals(newRequirement.getAttributes())) {
                return false;
            }
        }

        return true;
    }

    public void close() {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        try {
            if (!opened) {
                throw new IllegalStateException("Close was called on a Capability Collector that was already closed.");
            }
            opened = false;

            closeTracker();
            if (noItems()) {
                satisfied = false;
                capabilityConsumer.accept(suitings, satisfied);
            }
        } finally {
            writeLock.unlock();
        }
    }

    protected abstract void closeTracker();

    private Suiting<C>[] createSuitingsWithoutCapability(final RequirementDefinition<C>[] requirements) {
        @SuppressWarnings("unchecked")
        Suiting<C>[] lSuitings = new Suiting[requirements.length];
        for (int i = 0; i < requirements.length; i++) {
            lSuitings[i] = new Suiting<C>(requirements[i], null);
        }
        return lSuitings;
    }

    protected abstract C[] getAvailableCapabilities();

    public boolean isSatisfied() {
        return satisfied;
    }

    protected abstract boolean matches(C capability, Filter filter);

    protected void modifiedCapablility(final C capability) {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        boolean changed = false;
        for (int i = 0; i < suitings.length; i++) {
            Suiting<C> suiting = suitings[i];
            C suitedCapability = suiting.getCapability();
            if (capability.equals(suitedCapability) && !matches(capability, suiting.getRequirement().getFilter())) {
                changed = true;
                C newCapability = searchMatchingCapabilityForRequirement(suiting.getRequirement());
                suitings[i] = new Suiting<C>(suitings[i].getRequirement(), newCapability);
                satisfied = false;
            }
        }

        changed = changed || tryCapabilityOnUnsatisfiedRequirements(capability);

        if (changed) {
            capabilityConsumer.accept(suitings, satisfied);
        }

        writeLock.unlock();
    }

    private boolean noItems() {
        boolean result;
        ReadLock readLock = readWriteLock.readLock();
        readLock.lock();

        result = (suitings.length == 0);

        readLock.unlock();
        return result;
    }

    public void open() {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        try {
            if (opened) {
                throw new IllegalStateException("Open was called on a CapabilityCollector that was already opened.");
            }
            opened = true;

            if (noItems()) {
                this.satisfied = true;
            }
            capabilityConsumer.accept(suitings, satisfied);
            openTracker();
        } finally {
            writeLock.unlock();
        }
    }

    protected abstract void openTracker();

    protected void removedCapability(final C capability) {
        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        try {

            boolean lSatisfied = satisfied;
            boolean changed = false;

            for (int i = 0; i < suitings.length; i++) {
                Suiting<C> suiting = suitings[i];
                C suitedCapability = suiting.getCapability();
                if (capability.equals(suitedCapability)) {
                    changed = true;
                    RequirementDefinition<C> requirement = suiting.getRequirement();
                    C newCapability = searchMatchingCapabilityForRequirement(requirement);
                    suitings[i] = new Suiting<C>(requirement, newCapability);
                    if (newCapability == null) {
                        lSatisfied = false;
                    }
                }
            }

            this.satisfied = lSatisfied;

            if (changed) {
                capabilityConsumer.accept(suitings.clone(), this.satisfied);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private C searchMatchingCapabilityForRequirement(final RequirementDefinition<C> requirement) {
        if (!opened) {
            return null;
        }

        C[] availableCapabilities = getAvailableCapabilities();

        if (availableCapabilities == null) {
            return null;
        }

        C matchingCapability = null;
        for (int i = 0, n = availableCapabilities.length; (i < n) && (matchingCapability == null); i++) {
            Filter filter = requirement.getFilter();
            if (matches(availableCapabilities[i], filter)) {
                matchingCapability = availableCapabilities[i];
            }
        }
        return matchingCapability;
    }

    private boolean tryCapabilityOnUnsatisfiedRequirements(final C capability) {
        if (satisfied) {
            return false;
        }

        boolean changed = false;
        boolean lSatisfied = true;

        for (int i = 0; i < suitings.length; i++) {
            Suiting<C> suiting = suitings[i];

            if (suiting.getCapability() == null) {
                RequirementDefinition<C> requirement = suiting.getRequirement();
                if (matches(capability, requirement.getFilter())) {
                    changed = true;
                    suitings[i] = new Suiting<C>(requirement, capability);
                } else {
                    lSatisfied = false;
                }
            }
        }

        if (lSatisfied) {
            this.satisfied = true;
        }

        return changed;
    }

    /**
     * Updates the requirements of this collector. Those requirements that were already satisfied remain unchanged.
     *
     * @param newRequirements
     *            The list of reference items to be tracked.
     * @throws NullPointerException
     *             if any of the element of the item array is null.
     * @throws DuplicateRequirementIdException
     *             if two or more reference items contain the same id.
     */
    public void updateRequirements(final RequirementDefinition<C>[] newRequirements) {
        Objects.requireNonNull(newRequirements, "Items cannot be null");
        validateRequirements(newRequirements);

        WriteLock writeLock = readWriteLock.writeLock();
        writeLock.lock();

        try {
            if (areNewRequirementsSame(newRequirements)) {
                return;
            }

            if (!opened) {
                Suiting<C>[] newSuitings = createSuitingsWithoutCapability(newRequirements);
                this.suitings = newSuitings;
                return;
            }

            boolean lSatisfied = true;

            @SuppressWarnings("unchecked")
            Suiting<C>[] tmpSuitings = new Suiting[newRequirements.length];

            for (int i = 0; i < newRequirements.length; i++) {
                RequirementDefinition<C> requirement = newRequirements[i];
                C capability = searchMatchingCapabilityForRequirement(requirement);
                tmpSuitings[i] = new Suiting<C>(requirement, capability);
                if (capability == null) {
                    lSatisfied = false;
                }
            }

            this.satisfied = lSatisfied;
            this.suitings = tmpSuitings;

            capabilityConsumer.accept(suitings, satisfied);
        } finally {
            writeLock.unlock();
        }
    }

    private void validateRequirements(final RequirementDefinition<C>[] requirements) {
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
