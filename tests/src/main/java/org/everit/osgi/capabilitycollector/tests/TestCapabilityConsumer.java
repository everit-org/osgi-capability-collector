/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
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
package org.everit.osgi.capabilitycollector.tests;

import java.util.LinkedList;

import org.everit.osgi.capabilitycollector.CapabilityConsumer;
import org.everit.osgi.capabilitycollector.Suiting;

/**
 * Consumer implementation that remembers called functions.
 *
 * @param <C>
 *          The type of the capability.
 */
public class TestCapabilityConsumer<C> implements CapabilityConsumer<C> {

  private final LinkedList<CallParameters<C>> callHistory = new LinkedList<CallParameters<C>>();

  private boolean satisfied = false;

  @Override
  public void accept(final Suiting<C>[] suitings, final boolean pSatisfied) {
    callHistory.add(new CallParameters<C>(pSatisfied, suitings));
    this.satisfied = pSatisfied;
  }

  public void clearHistory() {
    callHistory.clear();
  }

  public boolean isSatisfied() {
    return satisfied;
  }

  /**
   * Polls the parameters of the next function call of the call history queue.
   */
  public CallParameters<C> pollCallParameters() {
    if (callHistory.isEmpty()) {
      return null;
    }
    return callHistory.removeFirst();
  }

}
