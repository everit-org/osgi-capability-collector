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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.capabilitycollector.DuplicateRequirementIdException;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.everit.osgi.capabilitycollector.ServiceReferenceCollector;
import org.everit.osgi.capabilitycollector.Suiting;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

@Component
@Properties({
        @Property(name = "eosgi.testId", value = "serviceCollectorTest"),
        @Property(name = "eosgi.testEngine", value = "junit4")
})
@Service(value = ServiceReferenceCollectorTestComponent.class)
@TestDuringDevelopment
public class ServiceReferenceCollectorTestComponent {

    private static final Map<String, Object> EMPTY_ATTRIBUTE_MAP = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    private static RequirementDefinition<ServiceReference<Object>>[] EMPTY_ITEMS = new RequirementDefinition[0];

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

    private Dictionary<String, Object> createServiceProps(String... strings) {
        Dictionary<String, Object> result = new Hashtable<String, Object>();
        for (int i = 0, n = strings.length; i < n; i = i + 2) {
            result.put(strings[i], strings[i + 1]);
        }
        return result;
    }

    @Test(expected = DuplicateRequirementIdException.class)
    public void testDuplicateReferenceItemId() {
        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("test", null, EMPTY_ATTRIBUTE_MAP),
                new RequirementDefinition<ServiceReference<Object>>("test", null, EMPTY_ATTRIBUTE_MAP) };

        new ServiceReferenceCollector<Object>(context, Object.class, items,
                new TestCapabilityConsumer<ServiceReference<Object>>(), false);
    }

    @Test
    @TestDuringDevelopment
    public void testNormalBehavior() {
        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>(
                        "test0", createFilter("(key=0)"), EMPTY_ATTRIBUTE_MAP),
                new RequirementDefinition<ServiceReference<Object>>(
                        "test0_s", createFilter("(&(key=0)(value=0))"), EMPTY_ATTRIBUTE_MAP),
                new RequirementDefinition<ServiceReference<Object>>(
                        "test1", createFilter("(key=1)"), EMPTY_ATTRIBUTE_MAP) };

        TestCapabilityConsumer<ServiceReference<Object>> capabilityConsumer =
                new TestCapabilityConsumer<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, items, capabilityConsumer, false);

        referenceTracker.open();

        CallParameters<ServiceReference<Object>> callParameters = capabilityConsumer.pollCallParameters();
        Assert.assertFalse(callParameters.isSatisfied());

        ServiceRegistration<Object> test0sSR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        Assert.assertFalse(capabilityConsumer.isSatisfied());
        callParameters = capabilityConsumer.pollCallParameters();
        Suiting<ServiceReference<Object>>[] suitings = callParameters.getSuitings();

        Assert.assertEquals("0", suitings[0].getCapability().getProperty("value"));
        Assert.assertEquals("0", suitings[1].getCapability().getProperty("value"));
        Assert.assertNull(suitings[2].getCapability());

        ServiceRegistration<Object> test1SR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "1"));

        Assert.assertTrue(capabilityConsumer.isSatisfied());

        ServiceRegistration<Object> test0SR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        capabilityConsumer.clearHistory();

        test0sSR.unregister();

        callParameters = capabilityConsumer.pollCallParameters();

        Assert.assertTrue(callParameters.isSatisfied());

        suitings = callParameters.getSuitings();
        Assert.assertEquals("0", suitings[0].getCapability().getProperty("value"));
        Assert.assertEquals("0", suitings[1].getCapability().getProperty("value"));
        Assert.assertEquals("1", suitings[2].getCapability().getProperty("key"));

        test0SR.setProperties(createServiceProps("key", "0", "value", "0", "x", "y"));

        Assert.assertTrue(capabilityConsumer.isSatisfied());

        test0SR.setProperties(createServiceProps("key", "5", "value", "0", "x", "y"));

        Assert.assertFalse(capabilityConsumer.isSatisfied());

        test0SR.setProperties(createServiceProps("key", "0", "value", "0", "x", "y"));

        Assert.assertTrue(capabilityConsumer.isSatisfied());

        ServiceRegistration<Object> test0sSR2 = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        capabilityConsumer.clearHistory();

        test0SR.setProperties(createServiceProps("key", "5", "value", "0", "x", "y"));

        callParameters = capabilityConsumer.pollCallParameters();

        Assert.assertFalse(callParameters.isSatisfied());

        // TODO check array

        test0sSR2.unregister();

        test1SR.unregister();

        test0SR.unregister();

        Assert.assertFalse(capabilityConsumer.isSatisfied());

        referenceTracker.close();

    }

    @Test(expected = NullPointerException.class)
    public void testNullCapabilityConsumer() {
        new ServiceReferenceCollector<Object>(context, Object.class, EMPTY_ITEMS, null, false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullAttributes() {
        new RequirementDefinition<Object>("test", createFilter("(1=1)"), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullContext() {
        new ServiceReferenceCollector<Object>(null, Object.class, EMPTY_ITEMS,
                new TestCapabilityConsumer<ServiceReference<Object>>(), false);
    }

    @Test
    public void testNullFilter() {

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("test", null, EMPTY_ATTRIBUTE_MAP) };

        TestCapabilityConsumer<ServiceReference<Object>> actionHandler =
                new TestCapabilityConsumer<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class,
                items, actionHandler, false);

        referenceTracker.open();
        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<Object> serviceRegistration = context.registerService(Object.class,
                new Object(), null);

        Assert.assertTrue(actionHandler.isSatisfied());
        serviceRegistration.unregister();
        Assert.assertFalse(actionHandler.isSatisfied());

        referenceTracker.close();

    }

    @Test(expected = NullPointerException.class)
    public void testNullItem() {
        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] { null };

        new ServiceReferenceCollector<Object>(context, Object.class, items,
                new TestCapabilityConsumer<ServiceReference<Object>>(), false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullItemId() {
        new RequirementDefinition<Object>(null, createFilter("(1=1)"), new HashMap<String, Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItems() {
        new ServiceReferenceCollector<Object>(context, Object.class, null,
                new TestCapabilityConsumer<ServiceReference<Object>>(), false);
    }

    @Test
    public void testNullReferenceType() {
        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<Object>("test0", createFilter("(key=0)"), EMPTY_ATTRIBUTE_MAP)
        };

        TestCapabilityConsumer<ServiceReference<Object>> actionHandler =
                new TestCapabilityConsumer<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> collector = new ServiceReferenceCollector<Object>(context, null, items,
                actionHandler, false);

        collector.open();

        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<Object> test0SR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        Assert.assertTrue(actionHandler.isSatisfied());

        // TODO check exact matching

        test0SR.unregister();

        Assert.assertFalse(actionHandler.isSatisfied());
    }

    @Test
    @TestDuringDevelopment
    public void testUpdateItemsEmptyItemsChange() {
        TestCapabilityConsumer<ServiceReference<Object>> actionHandler =
                new TestCapabilityConsumer<ServiceReference<Object>>();
        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, EMPTY_ITEMS, actionHandler, false);

        referenceTracker.open();

        Assert.assertTrue(referenceTracker.isSatisfied());

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<Object>("1", createFilter("(key=1)"), new HashMap<String, Object>()) };

        referenceTracker.updateRequirements(items);

        Assert.assertFalse(referenceTracker.isSatisfied());

        referenceTracker.updateRequirements(EMPTY_ITEMS);

        Assert.assertTrue(referenceTracker.isSatisfied());

        ServiceRegistration<Object> testSR1 = context.registerService(Object.class,
                new Object(), createServiceProps("key", "1"));

        referenceTracker.updateRequirements(items);

        Assert.assertTrue(referenceTracker.isSatisfied());

        referenceTracker.updateRequirements(EMPTY_ITEMS);

        Assert.assertTrue(referenceTracker.isSatisfied());

        testSR1.unregister();

        referenceTracker.close();

    }

    @Test(expected = NullPointerException.class)
    public void testUpdateItemsNullArray() {
        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, EMPTY_ITEMS, new TestCapabilityConsumer<ServiceReference<Object>>(), false);

        referenceTracker.updateRequirements(null);

    }

    @Test
    public void testUpdateItemsReplaceItemToExisting() {
        TestCapabilityConsumer<ServiceReference<Object>> actionHandler =
                new TestCapabilityConsumer<ServiceReference<Object>>();

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items1 = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("1", createFilter("(key=1)"),
                        new HashMap<String, Object>()) };

        ServiceRegistration<Object> testSR1 = context.registerService(Object.class,
                new Object(), createServiceProps("key", "1", "value", "1"));

        ServiceReferenceCollector<Object> collector = new ServiceReferenceCollector<Object>(context,
                Object.class, items1, actionHandler, false);

        collector.open();

        Assert.assertTrue(collector.isSatisfied());

        actionHandler.clearHistory();

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items2 = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("1", createFilter("(value=1)"),
                        new HashMap<String, Object>()) };

        collector.updateRequirements(items2);

        // TODO check updated method calls

        testSR1.unregister();

        collector.close();
    }

    @Test
    public void testUpdateItemsUnopenedTracker() {
        TestCapabilityConsumer<ServiceReference<Object>> actionHandler =
                new TestCapabilityConsumer<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, EMPTY_ITEMS, actionHandler, false);

        Assert.assertFalse(referenceTracker.isSatisfied());

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("1", createFilter("(key=1)"),
                        new HashMap<String, Object>()) };

        referenceTracker.updateRequirements(items);

        Assert.assertFalse(referenceTracker.isSatisfied());
        Assert.assertNull(actionHandler.pollCallParameters());

        referenceTracker.updateRequirements(EMPTY_ITEMS);

        Assert.assertFalse(referenceTracker.isSatisfied());
        Assert.assertNull(actionHandler.pollCallParameters());
    }

    /**
     * In case of zero items in the description, the "satisfied" action should be called as soon as the tracker is
     * opened and the "unsatisfied" action should be called as soon as the tracker is closed.
     */
    @Test
    public void testZeroItems() {
        TestCapabilityConsumer<ServiceReference<Object>> capabilityConsumer =
                new TestCapabilityConsumer<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> tracker = new ServiceReferenceCollector<Object>(context, Object.class,
                EMPTY_ITEMS, capabilityConsumer, false);

        Assert.assertFalse(capabilityConsumer.isSatisfied());

        tracker.open();

        Assert.assertTrue(capabilityConsumer.isSatisfied());

        tracker.close();

        Assert.assertFalse(capabilityConsumer.isSatisfied());
    }
}
