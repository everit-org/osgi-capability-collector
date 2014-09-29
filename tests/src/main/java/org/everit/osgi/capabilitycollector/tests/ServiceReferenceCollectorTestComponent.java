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
import org.everit.osgi.capabilitycollector.tests.TestActionHandler.MethodCallData;
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

        new ServiceReferenceCollector<Object>(context, Object.class, items, false,
                new TestActionHandler<ServiceReference<Object>>(), false);
    }

    @Test
    public void testNonSurvivor() {
        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>(
                        "test0", createFilter("(key=0)"), EMPTY_ATTRIBUTE_MAP),
                new RequirementDefinition<ServiceReference<Object>>(
                        "test0_s", createFilter("(&(key=0)(value=0))"), EMPTY_ATTRIBUTE_MAP),
                new RequirementDefinition<ServiceReference<Object>>(
                        "test1", createFilter("(key=1)"), EMPTY_ATTRIBUTE_MAP) };

        TestActionHandler<ServiceReference<Object>> actionHandler =
                new TestActionHandler<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class,
                items, false, actionHandler, false);

        referenceTracker.open();

        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<Object> test0sSR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        Assert.assertFalse(actionHandler.isSatisfied());
        Assert.assertEquals("0", actionHandler.getBinding("test0").getProperty("value"));
        Assert.assertEquals("0", actionHandler.getBinding("test0_s").getProperty("value"));
        Assert.assertFalse(actionHandler.containsBinding("test1"));

        ServiceRegistration<Object> test1SR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "1"));

        Assert.assertTrue(actionHandler.isSatisfied());

        ServiceRegistration<Object> test0SR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        actionHandler.clearCallHistory();

        test0sSR.unregister();

        MethodCallData methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_UNSATISFIED, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_UNBIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_UNBIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_SATISFIED, methodCall.getMethodName());

        test0SR.setProperties(createServiceProps("key", "0", "value", "0", "x", "y"));

        Assert.assertTrue(actionHandler.isSatisfied());

        test0SR.setProperties(createServiceProps("key", "5", "value", "0", "x", "y"));

        Assert.assertFalse(actionHandler.isSatisfied());

        test0SR.setProperties(createServiceProps("key", "0", "value", "0", "x", "y"));

        Assert.assertTrue(actionHandler.isSatisfied());

        ServiceRegistration<Object> test0sSR2 = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        actionHandler.clearCallHistory();

        test0SR.setProperties(createServiceProps("key", "5", "value", "0", "x", "y"));

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_UNSATISFIED, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_UNBIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_UNBIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_SATISFIED, methodCall.getMethodName());

        test0sSR2.unregister();

        test1SR.unregister();

        test0SR.unregister();

        Assert.assertFalse(actionHandler.isSatisfied());

        referenceTracker.close();

    }

    @Test(expected = NullPointerException.class)
    public void testNullActionHandler() {
        new ServiceReferenceCollector<Object>(context, Object.class, EMPTY_ITEMS, false, null, false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullAttributes() {
        new RequirementDefinition<Object>("test", createFilter("(1=1)"), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullContext() {
        new ServiceReferenceCollector<Object>(null, Object.class, EMPTY_ITEMS, false,
                new TestActionHandler<ServiceReference<Object>>(), false);
    }

    @Test
    @TestDuringDevelopment
    public void testNullFilter() {

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("test", null, EMPTY_ATTRIBUTE_MAP) };

        TestActionHandler<ServiceReference<Object>> actionHandler =
                new TestActionHandler<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class,
                items, false, actionHandler, false);

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

        new ServiceReferenceCollector<Object>(context, Object.class, items, false,
                new TestActionHandler<ServiceReference<Object>>(), false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullItemId() {
        new RequirementDefinition<Object>(null, createFilter("(1=1)"), new HashMap<String, Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItems() {
        new ServiceReferenceCollector<Object>(context, Object.class, null, false,
                new TestActionHandler<ServiceReference<Object>>(), false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullReferenceType() {
        new ServiceReferenceCollector<Object>(context, null, EMPTY_ITEMS, false,
                new TestActionHandler<ServiceReference<Object>>(), false);
    }

    @Test
    public void testSurvivor() {
        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<Object>("test0", createFilter("(key=0)"), EMPTY_ATTRIBUTE_MAP),
                new RequirementDefinition<Object>("test0_s", createFilter("(&(key=0)(value=0))"), EMPTY_ATTRIBUTE_MAP),
                new RequirementDefinition<Object>("test1", createFilter("(key=1)"), EMPTY_ATTRIBUTE_MAP) };

        TestActionHandler<ServiceReference<Object>> actionHandler = new TestActionHandler<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class,
                items, true, actionHandler, false);

        referenceTracker.open();

        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<Object> test0sSR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        Assert.assertFalse(actionHandler.isSatisfied());
        Assert.assertEquals("0", actionHandler.getBinding("test0").getProperty("value"));
        Assert.assertEquals("0", actionHandler.getBinding("test0_s").getProperty("value"));
        Assert.assertFalse(actionHandler.containsBinding("test1"));

        ServiceRegistration<Object> test1SR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "1"));

        Assert.assertTrue(actionHandler.isSatisfied());

        ServiceRegistration<Object> test0SR = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        actionHandler.clearCallHistory();

        test0sSR.unregister();

        MethodCallData methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertNull(methodCall);

        test0SR.setProperties(createServiceProps("key", "0", "value", "0", "x", "y"));

        Assert.assertTrue(actionHandler.isSatisfied());

        test0SR.setProperties(createServiceProps("key", "5", "value", "0", "x", "y"));

        Assert.assertFalse(actionHandler.isSatisfied());

        test0SR.setProperties(createServiceProps("key", "0", "value", "0", "x", "y"));

        Assert.assertTrue(actionHandler.isSatisfied());

        ServiceRegistration<Object> test0sSR2 = context.registerService(Object.class,
                new Object(), createServiceProps("key", "0", "value", "0"));

        actionHandler.clearCallHistory();

        test0SR.setProperties(createServiceProps("key", "5", "value", "0", "x", "y"));

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

        methodCall = actionHandler.pollMethodCallHistory();
        Assert.assertNull(methodCall);

        test0sSR2.unregister();

        test1SR.unregister();

        test0SR.unregister();

        Assert.assertFalse(actionHandler.isSatisfied());

        referenceTracker.close();
    }

    @Test
    public void testUpdateItemsEmptyChange() {
        testUpdateItemsEmptyItemsChange(true);
        testUpdateItemsEmptyItemsChange(false);
    }

    private void testUpdateItemsEmptyItemsChange(boolean survivor) {
        TestActionHandler<ServiceReference<Object>> actionHandler = new TestActionHandler<ServiceReference<Object>>();
        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, EMPTY_ITEMS, true, actionHandler, false);

        referenceTracker.open();

        Assert.assertTrue(referenceTracker.isSatisfied());

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<Object>("1", createFilter("(key=1)"), new HashMap<String, Object>()) };

        referenceTracker.updateItems(items);

        Assert.assertFalse(referenceTracker.isSatisfied());

        referenceTracker.updateItems(EMPTY_ITEMS);

        Assert.assertTrue(referenceTracker.isSatisfied());

        ServiceRegistration<Object> testSR1 = context.registerService(Object.class,
                new Object(), createServiceProps("key", "1"));

        referenceTracker.updateItems(items);

        Assert.assertTrue(referenceTracker.isSatisfied());

        referenceTracker.updateItems(EMPTY_ITEMS);

        Assert.assertTrue(referenceTracker.isSatisfied());

        testSR1.unregister();

        referenceTracker.close();

    }

    @Test(expected = NullPointerException.class)
    public void testUpdateItemsNullArray() {
        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, EMPTY_ITEMS, false, new TestActionHandler<ServiceReference<Object>>(), false);

        referenceTracker.updateItems(null);

    }

    @Test
    public void testUpdateItemsReplaceItemToExisting() {
        testUpdateItemsEmptyItemsChange(true);
        testUpdateItemsEmptyItemsChange(false);
    }

    public void testUpdateItemsReplaceItemToExisting(boolean survivor) {
        TestActionHandler<ServiceReference<Object>> actionHandler = new TestActionHandler<ServiceReference<Object>>();

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items1 = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("1", createFilter("(key=1)"),
                        new HashMap<String, Object>()) };

        ServiceRegistration<Object> testSR1 = context.registerService(Object.class,
                new Object(), createServiceProps("key", "1", "key", "2"));

        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, items1, survivor, actionHandler, false);

        Assert.assertTrue(referenceTracker.isSatisfied());

        referenceTracker.open();

        actionHandler.clearCallHistory();

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items2 = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("1", createFilter("(key=2)"),
                        new HashMap<String, Object>()) };

        referenceTracker.updateItems(items2);

        if (survivor) {
            MethodCallData methodCall = actionHandler.pollMethodCallHistory();
            Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());
        } else {
            MethodCallData methodCall = actionHandler.pollMethodCallHistory();
            Assert.assertEquals(TestActionHandler.METHOD_UNSATISFIED, methodCall.getMethodName());

            methodCall = actionHandler.pollMethodCallHistory();
            Assert.assertEquals(TestActionHandler.METHOD_UNBIND, methodCall.getMethodName());

            methodCall = actionHandler.pollMethodCallHistory();
            Assert.assertEquals(TestActionHandler.METHOD_BIND, methodCall.getMethodName());

            methodCall = actionHandler.pollMethodCallHistory();
            Assert.assertEquals(TestActionHandler.METHOD_SATISFIED, methodCall.getMethodName());
        }

        testSR1.unregister();

        referenceTracker.close();
    }

    @Test
    public void testUpdateItemsUnopenedTracker() {
        TestActionHandler<ServiceReference<Object>> actionHandler = new TestActionHandler<ServiceReference<Object>>();
        ServiceReferenceCollector<Object> referenceTracker = new ServiceReferenceCollector<Object>(context,
                Object.class, EMPTY_ITEMS, true, actionHandler, false);

        Assert.assertFalse(referenceTracker.isSatisfied());

        @SuppressWarnings("unchecked")
        RequirementDefinition<ServiceReference<Object>>[] items = new RequirementDefinition[] {
                new RequirementDefinition<ServiceReference<Object>>("1", createFilter("(key=1)"),
                        new HashMap<String, Object>()) };

        referenceTracker.updateItems(items);

        Assert.assertFalse(referenceTracker.isSatisfied());
        Assert.assertNull(actionHandler.pollMethodCallHistory());

        referenceTracker.updateItems(EMPTY_ITEMS);

        Assert.assertFalse(referenceTracker.isSatisfied());
        Assert.assertNull(actionHandler.pollMethodCallHistory());
    }

    /**
     * In case of zero items in the description, the "satisfied" action should be called as soon as the tracker is
     * opened and the "unsatisfied" action should be called as soon as the tracker is closed.
     */
    @Test
    public void testZeroItems() {
        TestActionHandler<ServiceReference<Object>> actionHandler = new TestActionHandler<ServiceReference<Object>>();

        ServiceReferenceCollector<Object> tracker = new ServiceReferenceCollector<Object>(context,
                Object.class,
                EMPTY_ITEMS, false, actionHandler, false);

        tracker.open();

        Assert.assertTrue(actionHandler.isSatisfied());

        tracker.close();

        Assert.assertFalse(actionHandler.isSatisfied());
    }
}
