/**
 * This file is part of Everit - OSGi Reference Tracker Tests.
 *
 * Everit - OSGi Reference Tracker Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Reference Tracker Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Reference Tracker Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.servicecollector.tests;

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
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.referencetracker.DuplicateReferenceItemIdException;
import org.everit.osgi.referencetracker.ReferenceItem;
import org.everit.osgi.referencetracker.ReferenceTracker;
import org.everit.osgi.servicecollector.tests.TestActionHandler.MethodCallData;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

@Component
@Properties({
        @Property(name = "eosgi.testId", value = "serviceCollectorTest"),
        @Property(name = "eosgi.testEngine", value = "junit4")
})
@Service(value = ReferenceTrackerTestComponent.class)
@TestDuringDevelopment
public class ReferenceTrackerTestComponent {

    private static final Map<String, Object> EMPTY_ATTRIBUTE_MAP = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    private static ReferenceItem<StringHolder>[] EMPTY_ITEMS = new ReferenceItem[0];

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

    @Test(expected = DuplicateReferenceItemIdException.class)
    public void testDuplicateReferenceItemId() {
        @SuppressWarnings("unchecked")
        ReferenceItem<Object>[] items = new ReferenceItem[] {
                new ReferenceItem<Object>("test", null, EMPTY_ATTRIBUTE_MAP),
                new ReferenceItem<Object>("test", null, EMPTY_ATTRIBUTE_MAP) };

        new ReferenceTracker<Object>(context, Object.class, items, false, new TestActionHandler<Object>());
    }

    @Test
    public void testNonSurvivor() {
        @SuppressWarnings("unchecked")
        ReferenceItem<StringHolder>[] items = new ReferenceItem[] {
                new ReferenceItem<Object>("test0", createFilter("(key=0)"), EMPTY_ATTRIBUTE_MAP),
                new ReferenceItem<Object>("test0_s", createFilter("(&(key=0)(value=0))"), EMPTY_ATTRIBUTE_MAP),
                new ReferenceItem<Object>("test1", createFilter("(key=1)"), EMPTY_ATTRIBUTE_MAP) };

        TestActionHandler<StringHolder> actionHandler = new TestActionHandler<StringHolder>();

        ReferenceTracker<StringHolder> referenceTracker = new ReferenceTracker<StringHolder>(context,
                StringHolder.class,
                items, false, actionHandler);

        referenceTracker.open(false);

        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<StringHolder> test0sSR = context.registerService(StringHolder.class,
                new StringHolder("test0_s"), createServiceProps("key", "0", "value", "0"));

        Assert.assertFalse(actionHandler.isSatisfied());
        Assert.assertTrue(actionHandler.getBinding("test0").getValue().equals("test0_s"));
        Assert.assertTrue(actionHandler.getBinding("test0_s").getValue().equals("test0_s"));
        Assert.assertFalse(actionHandler.containsBinding("test1"));

        ServiceRegistration<StringHolder> test1SR = context.registerService(StringHolder.class,
                new StringHolder("test1"), createServiceProps("key", "1"));

        Assert.assertTrue(actionHandler.isSatisfied());

        ServiceRegistration<StringHolder> test0SR = context.registerService(StringHolder.class,
                new StringHolder("test0_s_2"), createServiceProps("key", "0", "value", "0"));

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

        ServiceRegistration<StringHolder> test0sSR2 = context.registerService(StringHolder.class,
                new StringHolder("test0_s2"), createServiceProps("key", "0", "value", "0"));

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
        new ReferenceTracker<StringHolder>(context, StringHolder.class, EMPTY_ITEMS, false, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullAttributes() {
        new ReferenceItem<Object>("test", createFilter("(1=1)"), null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullContext() {
        new ReferenceTracker<StringHolder>(null, StringHolder.class, EMPTY_ITEMS, false,
                new TestActionHandler<StringHolder>());
    }

    @Test
    @TestDuringDevelopment
    public void testNullFilter() {

        @SuppressWarnings("unchecked")
        ReferenceItem<StringHolder>[] items = new ReferenceItem[] { new ReferenceItem<Object>("test", null,
                EMPTY_ATTRIBUTE_MAP) };

        TestActionHandler<StringHolder> actionHandler = new TestActionHandler<StringHolder>();

        ReferenceTracker<StringHolder> referenceTracker = new ReferenceTracker<StringHolder>(context,
                StringHolder.class,
                items, false, actionHandler);

        referenceTracker.open(false);
        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<StringHolder> serviceRegistration = context.registerService(StringHolder.class,
                new StringHolder(), null);

        Assert.assertTrue(actionHandler.isSatisfied());
        serviceRegistration.unregister();
        Assert.assertFalse(actionHandler.isSatisfied());

        referenceTracker.close();

    }

    @Test(expected = NullPointerException.class)
    public void testNullItem() {
        @SuppressWarnings("unchecked")
        ReferenceItem<Object>[] items = new ReferenceItem[] { null };

        new ReferenceTracker<Object>(context, Object.class, items, false, new TestActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItemId() {
        new ReferenceItem<Object>(null, createFilter("(1=1)"), new HashMap<String, Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItems() {
        new ReferenceTracker<Object>(context, Object.class, null, false, new TestActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullReferenceType() {
        new ReferenceTracker<StringHolder>(context, null, EMPTY_ITEMS, false, new TestActionHandler<StringHolder>());
    }

    @Test
    public void testSurvivor() {
        @SuppressWarnings("unchecked")
        ReferenceItem<StringHolder>[] items = new ReferenceItem[] {
                new ReferenceItem<Object>("test0", createFilter("(key=0)"), EMPTY_ATTRIBUTE_MAP),
                new ReferenceItem<Object>("test0_s", createFilter("(&(key=0)(value=0))"), EMPTY_ATTRIBUTE_MAP),
                new ReferenceItem<Object>("test1", createFilter("(key=1)"), EMPTY_ATTRIBUTE_MAP) };

        TestActionHandler<StringHolder> actionHandler = new TestActionHandler<StringHolder>();

        ReferenceTracker<StringHolder> referenceTracker = new ReferenceTracker<StringHolder>(context,
                StringHolder.class,
                items, true, actionHandler);

        referenceTracker.open(false);

        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<StringHolder> test0sSR = context.registerService(StringHolder.class,
                new StringHolder("test0_s"), createServiceProps("key", "0", "value", "0"));

        Assert.assertFalse(actionHandler.isSatisfied());
        Assert.assertTrue(actionHandler.getBinding("test0").getValue().equals("test0_s"));
        Assert.assertTrue(actionHandler.getBinding("test0_s").getValue().equals("test0_s"));
        Assert.assertFalse(actionHandler.containsBinding("test1"));

        ServiceRegistration<StringHolder> test1SR = context.registerService(StringHolder.class,
                new StringHolder("test1"), createServiceProps("key", "1"));

        Assert.assertTrue(actionHandler.isSatisfied());

        ServiceRegistration<StringHolder> test0SR = context.registerService(StringHolder.class,
                new StringHolder("test0_s_2"), createServiceProps("key", "0", "value", "0"));

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

        ServiceRegistration<StringHolder> test0sSR2 = context.registerService(StringHolder.class,
                new StringHolder("test0_s2"), createServiceProps("key", "0", "value", "0"));

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
        TestActionHandler<StringHolder> actionHandler = new TestActionHandler<StringHolder>();
        ReferenceTracker<StringHolder> referenceTracker = new ReferenceTracker<StringHolder>(context,
                StringHolder.class, EMPTY_ITEMS, true, actionHandler);

        referenceTracker.open(false);

        Assert.assertTrue(referenceTracker.isSatisfied());

        @SuppressWarnings("unchecked")
        ReferenceItem<StringHolder>[] items = new ReferenceItem[] {
                new ReferenceItem<StringHolder>("1", createFilter("(key=1)"), new HashMap<String, Object>()) };

        referenceTracker.updateItems(items);

        Assert.assertFalse(referenceTracker.isSatisfied());

        referenceTracker.updateItems(EMPTY_ITEMS);

        Assert.assertTrue(referenceTracker.isSatisfied());

        ServiceRegistration<StringHolder> testSR1 = context.registerService(StringHolder.class,
                new StringHolder("test"), createServiceProps("key", "1"));

        referenceTracker.updateItems(items);

        Assert.assertTrue(referenceTracker.isSatisfied());

        referenceTracker.updateItems(EMPTY_ITEMS);

        Assert.assertTrue(referenceTracker.isSatisfied());

        testSR1.unregister();

        referenceTracker.close();

    }

    @Test(expected = NullPointerException.class)
    public void testUpdateItemsNullArray() {
        ReferenceTracker<StringHolder> referenceTracker = new ReferenceTracker<StringHolder>(context,
                StringHolder.class, EMPTY_ITEMS, false, new TestActionHandler<StringHolder>());

        referenceTracker.updateItems(null);

    }

    @Test
    public void testUpdateItemsReplaceItemToExisting() {
        testUpdateItemsEmptyItemsChange(true);
        testUpdateItemsEmptyItemsChange(false);
    }

    public void testUpdateItemsReplaceItemToExisting(boolean survivor) {
        TestActionHandler<StringHolder> actionHandler = new TestActionHandler<StringHolder>();

        @SuppressWarnings("unchecked")
        ReferenceItem<StringHolder>[] items1 = new ReferenceItem[] {
                new ReferenceItem<StringHolder>("1", createFilter("(key=1)"), new HashMap<String, Object>()) };

        ServiceRegistration<StringHolder> testSR1 = context.registerService(StringHolder.class,
                new StringHolder("test"), createServiceProps("key", "1", "key", "2"));

        ReferenceTracker<StringHolder> referenceTracker = new ReferenceTracker<StringHolder>(context,
                StringHolder.class, items1, survivor, actionHandler);

        Assert.assertTrue(referenceTracker.isSatisfied());

        actionHandler.clearCallHistory();

        @SuppressWarnings("unchecked")
        ReferenceItem<StringHolder>[] items2 = new ReferenceItem[] {
                new ReferenceItem<StringHolder>("1", createFilter("(key=2)"), new HashMap<String, Object>()) };

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

        referenceTracker.open(false);

        referenceTracker.close();
    }

    @Test
    public void testUpdateItemsUnopenedTracker() {
        TestActionHandler<StringHolder> actionHandler = new TestActionHandler<StringHolder>();
        ReferenceTracker<StringHolder> referenceTracker = new ReferenceTracker<StringHolder>(context,
                StringHolder.class, EMPTY_ITEMS, true, actionHandler);

        Assert.assertFalse(referenceTracker.isSatisfied());

        @SuppressWarnings("unchecked")
        ReferenceItem<StringHolder>[] items = new ReferenceItem[] {
                new ReferenceItem<StringHolder>("1", createFilter("(key=1)"), new HashMap<String, Object>()) };

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
        TestActionHandler<StringHolder> actionHandler = new TestActionHandler<StringHolder>();

        ReferenceTracker<StringHolder> tracker = new ReferenceTracker<StringHolder>(context, StringHolder.class,
                EMPTY_ITEMS, false, actionHandler);

        tracker.open(false);

        Assert.assertTrue(actionHandler.isSatisfied());

        tracker.close();

        Assert.assertFalse(actionHandler.isSatisfied());
    }
}
