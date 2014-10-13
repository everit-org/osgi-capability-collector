package org.everit.osgi.capabilitycollector;

public class Suiting<C> {

    private final C capability;

    private final RequirementDefinition<C> requirement;

    public Suiting(RequirementDefinition<C> requirement, C capability) {
        this.requirement = requirement;
        this.capability = capability;
    }

    public C getCapability() {
        return capability;
    }

    public RequirementDefinition<C> getRequirement() {
        return requirement;
    }

}
