/**
 * This file is part of Everit - OSGi Capability Collector Tests.
 *
 * Everit - OSGi Capability Collector Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Capability Collector Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Capability Collector Tests.  If not, see <http://www.gnu.org/licenses/>.
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
import org.everit.osgi.capabilitycollector.tests.TestActionHandler.MethodCallData;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;

@Component
@Properties({
        @Property(name = "eosgi.testId", value = "bundleCollectorTest"),
        @Property(name = "eosgi.testEngine", value = "junit4")
})
@Service(value = BundleCapabilityCollectorTest.class)
public class BundleCapabilityCollectorTest {

    @SuppressWarnings("unchecked")
    private static RequirementDefinition<BundleCapability>[] EMPTY_REQUIREMENTS = new RequirementDefinition[0];

    private BundleContext context;

    @Activate
    public void activate(BundleContext context) {
        this.context = context;

    }

    private Filter createFilter(String filterString) {
        try {
            return context.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testBundleCollectorLogic() {
        Map<String, Object> zeroAttributes = new HashMap<String, Object>();
        zeroAttributes.put("zero", 0);

        Map<String, Object> oneAttributes = new HashMap<String, Object>();
        oneAttributes.put("zero", 0);

        @SuppressWarnings("unchecked")
        RequirementDefinition<BundleCapability>[] unsatisfiedRequirements = new RequirementDefinition[] {
                new RequirementDefinition<BundleCapability>("zero", createFilter("(zero=0)"), zeroAttributes),
                new RequirementDefinition<BundleCapability>("two", createFilter("(two=2)"), oneAttributes) };

        TestActionHandler<BundleCapability> actionHandler = new TestActionHandler<BundleCapability>();

        BundleCapabilityCollector collector = new BundleCapabilityCollector(context, "testNamespace",
                unsatisfiedRequirements, false, actionHandler, Bundle.ACTIVE);

        collector.open();

        @SuppressWarnings("unchecked")
        RequirementDefinition<BundleCapability>[] unsatisfiedRequirements2 = new RequirementDefinition[] {
                new RequirementDefinition<BundleCapability>("two", createFilter("(two=2)"), oneAttributes) };

        collector.updateItems(unsatisfiedRequirements2);

        @SuppressWarnings("unchecked")
        RequirementDefinition<BundleCapability>[] requirements = new RequirementDefinition[] {
                new RequirementDefinition<BundleCapability>("zero", createFilter("(zero=0)"), zeroAttributes),
                new RequirementDefinition<BundleCapability>("one", createFilter("(one=1)"), oneAttributes) };

        collector.updateItems(requirements);

        Assert.assertTrue(collector.isSatisfied());
        BundleCapability zeroBundleCapability = actionHandler.getBinding("zero");
        Assert.assertNotNull(zeroBundleCapability);
        Object zeroAttributeValue = zeroBundleCapability.getAttributes().get("zero");
        Assert.assertEquals(Long.valueOf(0), zeroAttributeValue);

        @SuppressWarnings("unchecked")
        RequirementDefinition<BundleCapability>[] newRequirements = new RequirementDefinition[] {
                new RequirementDefinition<BundleCapability>("one", createFilter("(one=1)"), oneAttributes) };

        actionHandler.clearCallHistory();
        collector.updateItems(newRequirements);

        MethodCallData methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals("unsatisfied", methodCall.getMethodName());
        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals("unbind", methodCall.getMethodName());

        @SuppressWarnings("unchecked")
        RequirementDefinition<BundleCapability> requirement = (RequirementDefinition<BundleCapability>) methodCall
                .getParams()[0];
        Assert.assertEquals("zero", requirement.getRequirementId());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals("satisfied", methodCall.getMethodName());

        collector.close();
    }

    @Test
    public void testStateMaskCheck() {
        new BundleCapabilityCollector(context, "testNamespace", EMPTY_REQUIREMENTS, false,
                new TestActionHandler<BundleCapability>(), Bundle.RESOLVED);
        new BundleCapabilityCollector(context, "testNamespace", EMPTY_REQUIREMENTS, false,
                new TestActionHandler<BundleCapability>(), Bundle.STARTING);
        new BundleCapabilityCollector(context, "testNamespace", EMPTY_REQUIREMENTS, false,
                new TestActionHandler<BundleCapability>(), Bundle.ACTIVE);
        new BundleCapabilityCollector(context, "testNamespace", EMPTY_REQUIREMENTS, false,
                new TestActionHandler<BundleCapability>(), Bundle.STOPPING);
        new BundleCapabilityCollector(context, "testNamespace", EMPTY_REQUIREMENTS, false,
                new TestActionHandler<BundleCapability>(), Bundle.STOPPING | Bundle.ACTIVE | Bundle.STARTING);

        try {
            new BundleCapabilityCollector(context, "testNamespace", EMPTY_REQUIREMENTS, false,
                    new TestActionHandler<BundleCapability>(), Bundle.INSTALLED);
            Assert.fail("Exception should have been thrown");
        } catch (RuntimeException e) {
            // Right behavior
        }
    }
}
