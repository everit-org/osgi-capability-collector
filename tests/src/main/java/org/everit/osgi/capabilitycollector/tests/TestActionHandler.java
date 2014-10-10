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
import java.util.LinkedList;
import java.util.Map;

import org.everit.osgi.capabilitycollector.CapabilityCollectorActionHandler;
import org.everit.osgi.capabilitycollector.RequirementDefinition;
import org.junit.Assert;

public final class TestActionHandler<R> implements CapabilityCollectorActionHandler<R> {

    public static class MethodCallData {

        private final String methodName;

        private final Object[] params;

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

    public static final String METHOD_BIND = "bind";

    public static final String METHOD_SATISFIED = "satisfied";

    public static final String METHOD_UNBIND = "unbind";

    public static final String METHOD_UNSATISFIED = "unsatisfied";

    private final Map<String, R> bindings = new HashMap<String, R>();

    private final LinkedList<MethodCallData> callHistory = new LinkedList<MethodCallData>();

    private boolean satisfied = false;

    @Override
    public void bind(RequirementDefinition<R> referenceItem, R reference) {
        callHistory.add(new MethodCallData(METHOD_BIND, referenceItem, reference));
        bindings.put(referenceItem.getRequirementId(), reference);
    }

    public void clearCallHistory() {
        callHistory.clear();
    }

    public boolean containsBinding(String referenceItemId) {
        return bindings.containsKey(referenceItemId);
    }

    public R getBinding(String referenceItemId) {
        return bindings.get(referenceItemId);
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public MethodCallData pollMethodCallHistory() {
        return callHistory.poll();
    }

    @Override
    public void satisfied() {
        callHistory.add(new MethodCallData(METHOD_SATISFIED));

        Assert.assertFalse(satisfied);

        satisfied = true;
    }

    @Override
    public void unbind(RequirementDefinition<R> referenceItemId) {
        callHistory.add(new MethodCallData(METHOD_UNBIND, referenceItemId));

        bindings.remove(referenceItemId);
    }

    @Override
    public void unsatisfied() {
        callHistory.add(new MethodCallData(METHOD_UNSATISFIED));

        Assert.assertTrue(satisfied);

        satisfied = false;
    }
}
