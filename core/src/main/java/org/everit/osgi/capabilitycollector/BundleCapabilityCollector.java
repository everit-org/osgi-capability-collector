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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * A Capability Collector that collects {@link BundleCapability}s.
 */
public class BundleCapabilityCollector extends AbstractCapabilityCollector<BundleCapability> {

  /**
   * Tracks all {@link BundleCapability}s in the system. In case there is a new capability, it is
   * passed to the {@link AbstractCapabilityCollector#addingCapablility(Object)} function. In case a
   * {@link BundleCapability} is removed,
   * {@link AbstractCapabilityCollector#removedCapability(Object)} is called.
   */
  private class TrackerCustomizer implements BundleTrackerCustomizer<Bundle> {

    @Override
    public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
      BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
      List<BundleCapability> capabilities = bundleWiring.getCapabilities(namespace);
      for (BundleCapability bundleCapability : capabilities) {
        addingCapablility(bundleCapability);
      }

      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      availableCapabilities.addAll(capabilities);
      writeLock.unlock();

      return bundle;
    }

    @Override
    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
      // Do nothing as this is only about bundle state change.
    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
      BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
      List<BundleCapability> capabilities = bundleWiring.getCapabilities(namespace);

      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      availableCapabilities.removeAll(capabilities);
      writeLock.unlock();

      for (BundleCapability bundleCapability : capabilities) {
        removedCapability(bundleCapability);
      }
    }

  }

  private final Set<BundleCapability> availableCapabilities = new HashSet<BundleCapability>();

  private final String namespace;

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private final BundleTracker<Bundle> tracker;

  /**
   * Constructor.
   *
   * @param context
   *          The context of the bundle that collects the capabilities.
   * @param namespace
   *          The namespace of the {@link BundleCapability}. See
   *          {@link BundleCapability#getNamespace()}.
   * @param requirements
   *          The definition of requirements. If all has a matching Capability, the collector
   *          becomes satisfied.
   * @param capabilityConsumer
   *          The consumer that will be called if there is a new matching Capability or one
   *          previously matched is not available anymore.
   * @param stateMask
   *          Only those {@link BundleCapability}s are tracked that belong to a {@link Bundle} that
   *          has any of the specified states. Supported states are {@link Bundle#RESOLVED},
   *          {@link Bundle#STARTING}, {@link Bundle#ACTIVE} and {@link Bundle#STOPPING}.
   */
  public BundleCapabilityCollector(final BundleContext context, final String namespace,
      final RequirementDefinition<BundleCapability>[] requirements,
      final CapabilityConsumer<BundleCapability> capabilityConsumer, final int stateMask) {
    super(requirements, capabilityConsumer);

    if ((~(Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE) & stateMask) > 0) {
      throw new IllegalArgumentException(
          "Only RESOLVED, STARTING, ACTIVE and STOPPING states"
              + " are allowed in the bundle stateMask: " + stateMask);
    }

    this.namespace = namespace;
    tracker = new BundleTracker<Bundle>(context, stateMask, new TrackerCustomizer());
  }

  @Override
  protected void closeTracker() {
    tracker.close();
  }

  @Override
  protected BundleCapability[] getAvailableCapabilities() {
    Lock readLock = readWriteLock.readLock();
    readLock.lock();
    BundleCapability[] result = availableCapabilities
        .toArray(new BundleCapability[availableCapabilities.size()]);
    readLock.unlock();
    return result;
  }

  @Override
  protected boolean matches(final BundleCapability capability, final Filter filter) {
    Map<String, Object> attributes = capability.getAttributes();
    return filter.matches(attributes);
  }

  @Override
  protected void openTracker() {
    tracker.open();
  }

}
