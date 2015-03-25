/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.biz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.capabilitycollector;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.annotation.Generated;

import org.osgi.framework.Filter;

/**
 * An abstract class that helps implementing custom capability collectors.
 *
 * @param <C>
 *          The type of the capability.
 */
public abstract class AbstractCapabilityCollector<C> {

  private final CapabilityConsumer<C> capabilityConsumer;

  private volatile boolean opened = false;

  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);

  private volatile boolean satisfied = false;

  private Suiting<C>[] suitings;

  /**
   * Constructor.
   *
   * @param requirements
   *          The definition of requirements that should be collected. The collector will be
   *          satisfied if there is one available capability for each requirement.
   * @param capabilityConsumer
   *          The consumer who is interested in the collected capabilities. This is a functional
   *          interface that has to be implemented by the user.
   */
  public AbstractCapabilityCollector(final RequirementDefinition<C>[] requirements,
      final CapabilityConsumer<C> capabilityConsumer) {

    Objects.requireNonNull(capabilityConsumer, "Capability consumer must not be null");
    Objects.requireNonNull(requirements, "Requirement item array must not be null");

    this.capabilityConsumer = capabilityConsumer;

    validateRequirements(requirements);

    Suiting<C>[] lSuitings = createSuitingsWithoutCapability(requirements);
    this.suitings = lSuitings;
  }

  /**
   * Should be called by the subclass when a new capability is available for the tracker.
   *
   * @param capability
   *          The capability that will be tried to satisfy each unsatisfied requirements.
   */
  protected void addingCapablility(final C capability) {
    WriteLock writeLock = readWriteLock.writeLock();
    writeLock.lock();

    try {
      boolean changed = tryCapabilityOnUnsatisfiedRequirements(capability);
      if (changed) {
        notifyConsumer();
      }
    } finally {
      writeLock.unlock();
    }
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

  /**
   * Closing the capability collector and frees up all resources. During the call of this method,
   * the tracker will be closed (and by closing the tracker, the consumer will be called with the
   * unsatisfied flag even if there are no requirements).
   */
  public void close() {
    WriteLock writeLock = readWriteLock.writeLock();
    writeLock.lock();

    try {
      if (!opened) {
        throw new IllegalStateException(
            "Close was called on a Capability Collector that was already closed.");
      }
      opened = false;

      closeTracker();
      if (noItems()) {
        satisfied = false;
        notifyConsumer();
      }
    } finally {
      writeLock.unlock();
    }
  }

  protected abstract void closeTracker();

  private Suiting<C>[] createSuitingsWithoutCapability(
      final RequirementDefinition<C>[] requirements) {

    @SuppressWarnings("unchecked")
    Suiting<C>[] lSuitings = new Suiting[requirements.length];
    for (int i = 0; i < requirements.length; i++) {
      lSuitings[i] = new Suiting<C>(requirements[i], null);
    }
    return lSuitings;
  }

  protected abstract C[] getAvailableCapabilities();

  @Generated("avoid_checkstyle_error_on_printStacktrace")
  private void handleConsumerError(final RuntimeException e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter pw = new PrintWriter(stringWriter);
    pw.write("Uncaughed exception during calling CapabilityConsumer.accept()"
        + " with the following parameters: [satisfied: " + String.valueOf(satisfied)
        + "; suitings: " + Arrays.toString(suitings) + "]\n");
    e.printStackTrace(pw);
    System.err.println(stringWriter.toString());
  }

  public boolean isOpened() {
    return opened;
  }

  public boolean isSatisfied() {
    return satisfied;
  }

  protected abstract boolean matches(C capability, Filter filter);

  /**
   * Should be called by the subclass if the already added capability is modified. In this case the
   * capability is re-tested agains all unsatisfied and wired requirements.
   *
   * @param capability
   *          The capability that is modified.
   */
  protected void modifiedCapablility(final C capability) {
    WriteLock writeLock = readWriteLock.writeLock();
    writeLock.lock();

    try {

      boolean changed = false;
      for (int i = 0; i < suitings.length; i++) {
        Suiting<C> suiting = suitings[i];
        C suitedCapability = suiting.getCapability();
        if (capability.equals(suitedCapability)
            && !matches(capability, suiting.getRequirement().getFilter())) {
          changed = true;
          C newCapability = searchMatchingCapabilityForRequirement(suiting.getRequirement());
          suitings[i] = new Suiting<C>(suitings[i].getRequirement(), newCapability);
          if (newCapability == null) {
            satisfied = false;
          }
        }
      }

      changed = changed || tryCapabilityOnUnsatisfiedRequirements(capability);

      if (changed) {
        notifyConsumer();
      }
    } catch (RuntimeException e) {
      handleConsumerError(e);
    } finally {
      writeLock.unlock();
    }
  }

  private boolean noItems() {
    return (suitings.length == 0);
  }

  private void notifyConsumer() {
    try {
      capabilityConsumer.accept(suitings.clone(), satisfied);
    } catch (RuntimeException e) {
      handleConsumerError(e);
    }
  }

  /**
   * Opens the capability collector. The tracker that is implemented by the subclass is also opened.
   */
  public void open() {
    WriteLock writeLock = readWriteLock.writeLock();
    writeLock.lock();

    try {
      if (opened) {
        throw new IllegalStateException(
            "Open was called on a CapabilityCollector that was already opened.");
      }
      opened = true;

      if (noItems()) {
        this.satisfied = true;
      }
      notifyConsumer();
      openTracker();
    } finally {
      writeLock.unlock();
    }
  }

  protected abstract void openTracker();

  /**
   * This method should be called by the subclass if a capability that was previously added, is not
   * available anymore.
   *
   * @param capability
   *          The capability that is removed.
   */
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
        notifyConsumer();
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
    for (int i = 0, n = availableCapabilities.length; (i < n)
        && (matchingCapability == null); i++) {
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
   * Updates the requirements of this collector. Those requirements that were already satisfied
   * remain unchanged.
   *
   * @param newRequirements
   *          The list of reference items to be tracked.
   * @throws NullPointerException
   *           if any of the element of the item array is null.
   * @throws DuplicateRequirementIdException
   *           if two or more reference items contain the same id.
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

      notifyConsumer();
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
        throw new DuplicateRequirementIdException("The requirement id '"
            + requirement.getRequirementId()
            + "' is duplicated in the collector");
      }
    }
  }
}
