package jmri.jmrix.roco.z21;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The class being tested only has one reporter, hence some tests pulled down.
 *
 * @author Paul Bender Copyright (C) 2016
 **/

public class Z21ReporterManagerTest extends jmri.managers.AbstractReporterMgrTestBase {

    @Override
    public String getSystemName(String i) {
        return "ZR" + i;
    }

   @Before
    @Override
   public void setUp(){
        apps.tests.Log4JFixture.setUp();
        jmri.util.JUnitUtil.resetInstanceManager();
        jmri.util.JUnitUtil.initDefaultUserMessagePreferences();
        Z21SystemConnectionMemo memo = new Z21SystemConnectionMemo();
        Z21InterfaceScaffold tc = new Z21InterfaceScaffold();
        memo.setTrafficController(tc);
        memo.setRocoZ21CommandStation(new RocoZ21CommandStation());
        l = new Z21ReporterManager(memo);
   }

   @After
   public void tearDown(){
        apps.tests.Log4JFixture.tearDown();
        jmri.util.JUnitUtil.resetInstanceManager();
   }

    @Override
    protected int maxN() { return 1; }
    
    @Override
    protected String getNameToTest1() {
        return "1";
    }

}
