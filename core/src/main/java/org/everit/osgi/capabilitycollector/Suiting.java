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

/**
 * A suiting describes the satisfaction (or dissatisfaction) of a requirement. In case the
 * requirement is unsatisfied, the capability attribute is <code>null</code>.
 *
 * @param <C>
 *          The type of the capability.
 */
public class Suiting<C> {

  private final C capability;

  private final RequirementDefinition<C> requirement;

  public Suiting(final RequirementDefinition<C> requirement, final C capability) {
    this.requirement = requirement;
    this.capability = capability;
  }

  /**
   * The capability that satisfies the requirement. In case the requirement is not satisfied, the
   * capability is <code>null</code>.
   *
   * @return the capability that satisfies the requirement or <code>null</code> if the requirement
   *         is not satisfied.
   */
  public C getCapability() {
    return capability;
  }

  /**
   * The requirement that is satisfied. In case the requirement is not satisfied, the capability is
   * <code>null</code>.
   */
  public RequirementDefinition<C> getRequirement() {
    return requirement;
  }

}
