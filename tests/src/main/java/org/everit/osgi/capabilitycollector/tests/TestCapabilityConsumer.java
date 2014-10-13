package org.everit.osgi.capabilitycollector.tests;

import java.util.LinkedList;

import org.everit.osgi.capabilitycollector.CapabilityConsumer;
import org.everit.osgi.capabilitycollector.Suiting;

public class TestCapabilityConsumer<C> implements CapabilityConsumer<C> {

    private final LinkedList<CallParameters<C>> callHistory = new LinkedList<CallParameters<C>>();

    private boolean satisfied = false;

    @Override
    public void accept(Suiting<C>[] suitings, Boolean satisfied) {
        callHistory.add(new CallParameters<C>(satisfied, suitings));
        this.satisfied = satisfied;
    }

    public void clearHistory() {
        callHistory.clear();
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public CallParameters<C> pollCallParameters() {
        if (callHistory.isEmpty()) {
            return null;
        }
        return callHistory.removeFirst();
    }

}
