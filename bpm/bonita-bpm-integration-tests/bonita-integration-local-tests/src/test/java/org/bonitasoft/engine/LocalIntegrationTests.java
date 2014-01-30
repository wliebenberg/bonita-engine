package org.bonitasoft.engine;

import org.bonitasoft.engine.BonitaSuiteRunner.Initializer;
import org.bonitasoft.engine.event.ErrorEventSubProcessTest;
import org.bonitasoft.engine.event.SignalEventSubProcessTest;
import org.bonitasoft.engine.process.ProcessDeletionTest;
import org.bonitasoft.engine.search.SearchProcessInstanceTest;
import org.bonitasoft.engine.test.APIMethodTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(BonitaSuiteRunner.class)
@SuiteClasses({ ProcessDeletionTest.class, SearchProcessInstanceTest.class, SignalEventSubProcessTest.class, ErrorEventSubProcessTest.class,
        APIMethodTest.class })
@Initializer(TestsInitializer.class)
public class LocalIntegrationTests {

    @BeforeClass
    public static void beforeClass() {
        System.err.println("=================== LocalIntegrationTests setup");
    }

    @AfterClass
    public static void afterClass() {
        System.err.println("=================== LocalIntegrationTests teardown");
    }

}
