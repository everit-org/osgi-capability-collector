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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Filter;

/**
 * The definition of a requirement.
 *
 * @param <C>
 *          Type of capability that satisfies this requirement.
 */
public class RequirementDefinition<C> {

  private final Map<String, Object> attributes;

  private final Filter filter;

  private final String requirementId;

  /**
   * Constructor of {@link RequirementDefinition}.
   *
   * @param requirementId
   *          Id of the requirement that must be unique within the same collector.
   * @param filter
   *          A capability can satisfy this requirement if the filter matches the capability.
   * @param attributes
   *          Additional metadata that can be used by the {@link CapabilityConsumer} implementation.
   */
  public RequirementDefinition(final String requirementId, final Filter filter,
      final Map<String, Object> attributes) {
    Objects.requireNonNull(requirementId, "Requirement id must be provided");
    Objects.requireNonNull(attributes,
        "Attributes for requirement must be provided at least with a zero element map");

    this.requirementId = requirementId;
    this.filter = filter;
    this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(attributes));
  }

  /**
   * Additional metadata that can be used by the {@link CapabilityConsumer} implementation.
   */
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * A capability can satisfy this requirement if the filter matches the capability.
   */
  public Filter getFilter() {
    return filter;
  }

  /**
   * Id of the requirement that must be unique within the same collector.
   */
  public String getRequirementId() {
    return requirementId;
  }

  @Override
  public String toString() {
    return "RequirementDefinition [requirementId=" + requirementId + ", filter=" + filter
        + ", attributes="
        + attributes + "]";
  }

}
