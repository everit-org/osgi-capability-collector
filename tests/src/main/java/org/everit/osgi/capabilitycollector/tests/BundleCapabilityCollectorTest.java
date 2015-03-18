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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.capabilitycollector.BundleCapabilityCollector;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;

/**
 * Test component of bundle capability collector.
 */
@Component
@Properties({
    @Property(name = "eosgi.testId", value = "bundleCollectorTest"),
    @Property(name = "eosgi.testEngine", value = "junit4") })
@Service(value = BundleCapabilityCollectorTest.class)
@TestDuringDevelopment
public class BundleCapabilityCollectorTest {

  @SuppressWarnings("unchecked")
  private static final RequirementDefinition<BundleCapability>[] EMPTY_REQUIREMENTS =
      new RequirementDefinition[0];

  private static final String TEST_NAMESPACE = "testNamespace";

  private BundleContext context;

  @Activate
  public void activate(final BundleContext pContext) {
    this.context = pContext;

  }

  private Filter createFilter(final String filterString) {
    try {
      return context.createFilter(filterString);
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testBundleCollectorLogic() {
    Map<String, Object> zeroAttributes = new HashMap<String, Object>();

    final String testZeroAttribute = "zero";
    final String testOneAttribute = "one";
    final String oneEqOneFilter = '(' + testOneAttribute + "=1)";
    final String testTwoAttribute = "two";
    final String twoEqTwoFilter = '(' + testTwoAttribute + "=2)";

    zeroAttributes.put(testZeroAttribute, 0);

    Map<String, Object> oneAttributes = new HashMap<String, Object>();
    oneAttributes.put(testZeroAttribute, 0);

    String zeroEqZeroFilter = "(" + testZeroAttribute + "=0)";

    @SuppressWarnings("unchecked")
    RequirementDefinition<BundleCapability>[] unsatisfiedRequirements =
        new RequirementDefinition[] {
            new RequirementDefinition<BundleCapability>(testZeroAttribute,
                createFilter(zeroEqZeroFilter),
                zeroAttributes),
            new RequirementDefinition<BundleCapability>(testTwoAttribute,
                createFilter(twoEqTwoFilter),
                oneAttributes) };

    TestCapabilityConsumer<BundleCapability> capabilityConsumer =
        new TestCapabilityConsumer<BundleCapability>();

    BundleCapabilityCollector collector = new BundleCapabilityCollector(context, TEST_NAMESPACE,
        unsatisfiedRequirements, capabilityConsumer, Bundle.ACTIVE);

    collector.open();

    @SuppressWarnings("unchecked")
    RequirementDefinition<BundleCapability>[] unsatisfiedRequirements2 =
        new RequirementDefinition[] {
            new RequirementDefinition<BundleCapability>(testTwoAttribute,
                createFilter(twoEqTwoFilter),
                oneAttributes) };

    collector.updateRequirements(unsatisfiedRequirements2);

    @SuppressWarnings("unchecked")
    RequirementDefinition<BundleCapability>[] requirements = new RequirementDefinition[] {
        new RequirementDefinition<BundleCapability>(testZeroAttribute,
            createFilter(zeroEqZeroFilter),
            zeroAttributes),
        new RequirementDefinition<BundleCapability>(testOneAttribute, createFilter(oneEqOneFilter),
            oneAttributes) };

    capabilityConsumer.clearHistory();

    collector.updateRequirements(requirements);

    Assert.assertTrue(collector.isSatisfied());
    BundleCapability zeroBundleCapability = capabilityConsumer.pollCallParameters().suitings[0]
        .getCapability();

    Assert.assertNotNull(zeroBundleCapability);
    Object zeroAttributeValue = zeroBundleCapability.getAttributes().get(testZeroAttribute);
    Assert.assertEquals(Long.valueOf(0), zeroAttributeValue);

    @SuppressWarnings("unchecked")
    RequirementDefinition<BundleCapability>[] newRequirements = new RequirementDefinition[] {
        new RequirementDefinition<BundleCapability>(testOneAttribute, createFilter(oneEqOneFilter),
            oneAttributes) };

    capabilityConsumer.clearHistory();
    collector.updateRequirements(newRequirements);

    CallParameters<BundleCapability> methodCall = capabilityConsumer.pollCallParameters();

    RequirementDefinition<BundleCapability> requirement = methodCall.suitings[0]
        .getRequirement();
    Assert.assertEquals(testOneAttribute, requirement.getRequirementId());

    collector.close();
  }

  @Test
  public void testStateMaskCheck() {
    new BundleCapabilityCollector(context, TEST_NAMESPACE, EMPTY_REQUIREMENTS,
        new TestCapabilityConsumer<BundleCapability>(), Bundle.RESOLVED);
    new BundleCapabilityCollector(context, TEST_NAMESPACE, EMPTY_REQUIREMENTS,
        new TestCapabilityConsumer<BundleCapability>(), Bundle.STARTING);
    new BundleCapabilityCollector(context, TEST_NAMESPACE, EMPTY_REQUIREMENTS,
        new TestCapabilityConsumer<BundleCapability>(), Bundle.ACTIVE);
    new BundleCapabilityCollector(context, TEST_NAMESPACE, EMPTY_REQUIREMENTS,
        new TestCapabilityConsumer<BundleCapability>(), Bundle.STOPPING);
    new BundleCapabilityCollector(context, TEST_NAMESPACE, EMPTY_REQUIREMENTS,
        new TestCapabilityConsumer<BundleCapability>(), Bundle.STOPPING | Bundle.ACTIVE
            | Bundle.STARTING);

    try {
      new BundleCapabilityCollector(context, TEST_NAMESPACE, EMPTY_REQUIREMENTS,
          new TestCapabilityConsumer<BundleCapability>(), Bundle.INSTALLED);
      Assert.fail("Exception should have been thrown");
    } catch (RuntimeException e) {
      // Right behavior
    }
  }
}
