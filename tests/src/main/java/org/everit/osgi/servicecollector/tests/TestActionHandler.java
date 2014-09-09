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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.everit.osgi.servicecollector.ReferenceActionHandler;
import org.everit.osgi.servicecollector.ReferenceItem;
import org.junit.Assert;
import org.osgi.framework.ServiceReference;

public final class TestActionHandler<S> implements ReferenceActionHandler<S> {

    public static final String METHOD_UNBIND = "unbind";

    public static final String METHOD_UNSATISFIED = "unsatisfied";

    public static final String METHOD_SATISFIED = "satisfied";

    public static final String METHOD_BIND = "bind";

    public static class MethodCallData {

        private String methodName;

        private Object[] params;

        public MethodCallData(String methodName, Object... params) {
            this.methodName = methodName;
            this.params = params;
        }

        public String getMethodName() {
            return methodName;
        }

        public Object[] getParams() {
            return params;
        }
    }

    private boolean satisfied = false;

    private Map<String, S> bindings = new HashMap<String, S>();

    private LinkedList<MethodCallData> callHistory = new LinkedList<MethodCallData>();

    @Override
    public void bind(ReferenceItem<S> referenceItem, ServiceReference<S> reference, S service, int position) {
        callHistory.add(new MethodCallData(METHOD_BIND, referenceItem, reference, service));
        bindings.put(referenceItem.getReferenceItemId(), service);
    }

    @Override
    public void satisfied() {
        callHistory.add(new MethodCallData(METHOD_SATISFIED));

        Assert.assertFalse(satisfied);

        satisfied = true;
    }

    @Override
    public void unsatisfied() {
        callHistory.add(new MethodCallData(METHOD_UNSATISFIED));

        Assert.assertTrue(satisfied);

        satisfied = false;
    }

    @Override
    public void unbind(ReferenceItem<S> referenceItemId) {
        callHistory.add(new MethodCallData(METHOD_UNBIND, referenceItemId));

        bindings.remove(referenceItemId);
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public MethodCallData pollMethodCallHistory() {
        return callHistory.poll();
    }

    public boolean containsBinding(String referenceItemId) {
        return bindings.containsKey(referenceItemId);
    }

    public S getBinding(String referenceItemId) {
        return bindings.get(referenceItemId);
    }

    public void clearCallHistory() {
        callHistory.clear();
    }
}
