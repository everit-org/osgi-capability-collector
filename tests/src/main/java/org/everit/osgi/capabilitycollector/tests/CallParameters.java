package org.everit.osgi.capabilitycollector.tests;

import org.everit.osgi.capabilitycollector.Suiting;

public class CallParameters<C> {

    private final boolean satisfied;

    private final Suiting<C>[] suitings;

    public CallParameters(boolean satisfied, Suiting<C>[] suitings) {
        this.satisfied = satisfied;
        this.suitings = suitings;
    }

    public Suiting<C>[] getSuitings() {
        return suitings;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

}
