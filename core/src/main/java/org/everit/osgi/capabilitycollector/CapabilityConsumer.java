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

import aQute.bnd.annotation.ConsumerType;

/**
 * The consumer must be implemented by the programmer who uses this library. The consumer is
 * notified each time
 * <ul>
 * <li>a new capability is available that matches an unsatisfied requirement</li>
 * <li>a capability does not satisfy a requirement anymore due to modification</li>
 * <li>a capability is removed that made a requirement satisfied</li>
 * </ul>
 *
 * @param <C>
 *          The type of the capability.
 */
@ConsumerType
public interface CapabilityConsumer<C> {

  /**
   * The function that is called if there is any state change in the suitings or the collector. The
   * programmer who uses this library must implement this function.
   *
   * @param suitings
   *          The suitings that are available in this collector from now on.
   * @param satisfied
   *          True if all suitings are satisfied. The function will be also called with satisfied
   *          false if there are no requirements at all but the close function of the capability is
   *          called.
   */
  void accept(Suiting<C>[] suitings, boolean satisfied);
}
