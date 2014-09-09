/**
 * This file is part of Everit - OSGi Service Collector Tests.
 *
 * Everit - OSGi Service Collector Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Service Collector Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Service Collector Tests.  If not, see <http://www.gnu.org/licenses/>.
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
import org.everit.osgi.servicecollector.DuplicateReferenceItemIdException;
import org.everit.osgi.servicecollector.ReferenceItem;
import org.everit.osgi.servicecollector.ReferenceTracker;
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

    private BundleContext context;

    @SuppressWarnings("unchecked")
    private static ReferenceItem<Object>[] EMPTY_ITEMS = new ReferenceItem[0];

    @Activate
    public void activate(BundleContext context) {
        this.context = context;

    }

    @Test(expected = NullPointerException.class)
    public void testNullContext() {
        new ReferenceTracker<Object>(null, Object.class, EMPTY_ITEMS, false, new TestActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullActionHandler() {
        new ReferenceTracker<Object>(context, Object.class, EMPTY_ITEMS, false, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullReferenceType() {
        new ReferenceTracker<Object>(context, null, EMPTY_ITEMS, false, new TestActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItems() {
        new ReferenceTracker<Object>(context, Object.class, null, false, new TestActionHandler<Object>());
    }

    /**
     * In case of zero items in the description, the "satisfied" action should be called as soon as the tracker is
     * opened and the "unsatisfied" action should be called as soon as the tracker is closed.
     */
    @Test
    public void testZeroItems() {
        TestActionHandler<Object> actionHandler = new TestActionHandler<Object>();

        ReferenceTracker<Object> tracker = new ReferenceTracker<Object>(context, Object.class, EMPTY_ITEMS, false,
                actionHandler);

        tracker.open(false);

        Assert.assertTrue(actionHandler.isSatisfied());

        tracker.close();

        Assert.assertFalse(actionHandler.isSatisfied());
    }

    @Test(expected = DuplicateReferenceItemIdException.class)
    public void testDuplicateReferenceItemId() {
        @SuppressWarnings("unchecked")
        ReferenceItem<Object>[] items = new ReferenceItem[] {
                new ReferenceItem<Object>("test", null, EMPTY_ATTRIBUTE_MAP),
                new ReferenceItem<Object>("test", null, EMPTY_ATTRIBUTE_MAP) };

        new ReferenceTracker<Object>(context, Object.class, items, false, new TestActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItem() {
        @SuppressWarnings("unchecked")
        ReferenceItem<Object>[] items = new ReferenceItem[] { null };

        new ReferenceTracker<Object>(context, Object.class, items, false, new TestActionHandler<Object>());
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
    public void testNullItemId() {
        new ReferenceItem<Object>(null, createFilter("(1=1)"), new HashMap<String, Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullAttributes() {
        new ReferenceItem<Object>("test", createFilter("(1=1)"), null);
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
}
