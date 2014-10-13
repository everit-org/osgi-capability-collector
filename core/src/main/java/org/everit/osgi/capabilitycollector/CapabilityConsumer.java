package org.everit.osgi.capabilitycollector;

public interface CapabilityConsumer<C> {

    /**
     *
     * @param t
     */
    public void accept(Suiting<C>[] suitings, Boolean satisfied);
}
