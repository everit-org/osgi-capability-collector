package org.everit.osgi.servicecollector.tests;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.junit.Test;

@Component
@Properties({
        @Property(name = "eosgi.testId", value = "serviceCollectorTest"),
        @Property(name = "eosgi.testEngine", value = "junit4")
})
@Service(value = ServiceCollectorTestComponent.class)
public class ServiceCollectorTestComponent {

    @Test
    @TestDuringDevelopment
    public void test() {

    }

}
