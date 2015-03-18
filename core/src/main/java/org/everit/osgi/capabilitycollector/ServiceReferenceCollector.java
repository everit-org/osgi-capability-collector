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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * An {@link AbstractCapabilityCollector} implementation that collects {@link ServiceReference}s.
 *
 * @param <S>
 *          Type of the service.
 */
public class ServiceReferenceCollector<S> extends AbstractCapabilityCollector<ServiceReference<S>> {

  /**
   * Customizer of the inner {@link ServiceTracker}.
   */
  private class ReferenceTrackerCustomizer implements
      ServiceTrackerCustomizer<S, ServiceReference<S>> {

    @Override
    public ServiceReference<S> addingService(final ServiceReference<S> reference) {
      addingCapablility(reference);
      return reference;
    }

    @Override
    public void modifiedService(final ServiceReference<S> reference,
        final ServiceReference<S> tracked) {
      modifiedCapablility(reference);
    }

    @Override
    public void removedService(final ServiceReference<S> reference,
        final ServiceReference<S> tracked) {
      removedCapability(reference);
    }
  }

  private final boolean trackAllServices;

  private final ServiceTracker<S, ServiceReference<S>> tracker;

  /**
   * Constructor.
   *
   * @param context
   *          The context of the bundle that is used to track the services.
   * @param referenceType
   *          The type of the reference
   * @param requirements
   *          The requirements that need to be satisfied to satisfy the collector. The filter of
   *          each requirement must match a {@link ServiceReference}.
   * @param capabilityConsumer
   *          The consumer must be implemented by the programmer using this class to get notified
   *          about the events of the collector.
   * @param trackAllServices
   *          If {@code true}, then this {@code collector} will track all matching services
   *          regardless of class loader accessibility. If {@code false}, then this
   *          {@code collector} will only track matching services which are class loader accessible
   *          to the bundle whose {@code BundleContext} is used by this {@code collector}.
   */
  public ServiceReferenceCollector(final BundleContext context, final Class<S> referenceType,
      final RequirementDefinition<ServiceReference<S>>[] requirements,
      final CapabilityConsumer<ServiceReference<S>> capabilityConsumer,
      final boolean trackAllServices) {
    super(requirements, capabilityConsumer);

    this.trackAllServices = trackAllServices;
    if (referenceType == null) {
      try {
        tracker = new ServiceTracker<S, ServiceReference<S>>(context,
            context.createFilter("(service.id=*)"),
            new ReferenceTrackerCustomizer());
      } catch (InvalidSyntaxException e) {
        throw new RuntimeException(e);
      }
    } else {
      tracker = new ServiceTracker<S, ServiceReference<S>>(context, referenceType,
          new ReferenceTrackerCustomizer());
    }
  }

  @Override
  protected void closeTracker() {
    tracker.close();
  }

  @Override
  protected ServiceReference<S>[] getAvailableCapabilities() {
    return tracker.getServiceReferences();
  }

  @Override
  protected boolean matches(final ServiceReference<S> capability, final Filter filter) {
    return (filter == null) || filter.match(capability);
  }

  @Override
  protected void openTracker() {
    tracker.open(trackAllServices);
  }
}
