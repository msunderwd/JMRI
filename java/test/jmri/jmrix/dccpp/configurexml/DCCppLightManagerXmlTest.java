package jmri.jmrix.dccpp.configurexml;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * DCCppLightManagerXmlTest.java
 *
 * Description: tests for the DCCppLightManagerXml class
 *
 * @author   Paul Bender  Copyright (C) 2016
 */
public class DCCppLightManagerXmlTest {

    @Test
    public void testCtor(){
      Assert.assertNotNull("DCCppLightManagerXml constructor",new DCCppLightManagerXml());
    }

    // The minimal setup for log4J
    @Before
    public void setUp() {
        apps.tests.Log4JFixture.setUp();
    }

    @After
    public void tearDown() {
        apps.tests.Log4JFixture.tearDown();
    }

}

