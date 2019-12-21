package jmri.jmrit.logixng.analog.expressions;

import java.util.Locale;
import jmri.AnalogIO;
import jmri.JmriException;
import jmri.NamedBean;
import jmri.jmrit.logixng.AnalogExpressionBean;
import jmri.jmrit.logixng.AbstractBaseTestBase;
import jmri.jmrit.logixng.analog.implementation.DefaultMaleAnalogExpressionSocket.AnalogExpressionDebugConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test AbstractAnalogExpression
 * 
 * @author Daniel Bergqvist 2018
 */
public abstract class AbstractAnalogExpressionTestBase extends AbstractBaseTestBase {

    @Test
    public void testBundle() {
        Assert.assertEquals("strings are equal", "Get memory", Bundle.getMessage("AnalogExpressionMemory0"));
        Assert.assertEquals("strings are equal", "Get memory IM1", Bundle.getMessage("AnalogExpressionMemory1", "IM1"));
        Assert.assertEquals("strings are equal", "Get memory", Bundle.getMessage(Locale.CANADA, "AnalogExpressionMemory0"));
        Assert.assertEquals("strings are equal", "Get memory IM1", Bundle.getMessage(Locale.CANADA, "AnalogExpressionMemory1", "IM1"));
    }
    
    @Test
    public void testGetBeanType() {
        Assert.assertTrue("String matches", "Analog expression".equals(((AnalogExpressionBean)_base).getBeanType()));
    }
    
    @Test
    public void testState() throws JmriException {
        AnalogExpressionBean _expression = (AnalogExpressionBean)_base;
        _expression.setState(AnalogIO.INCONSISTENT);
        Assert.assertTrue("State matches", AnalogIO.INCONSISTENT == _expression.getState());
        jmri.util.JUnitAppender.assertWarnMessage("Unexpected call to getState in AbstractAnalogExpression.");
        _expression.setState(AnalogIO.UNKNOWN);
        Assert.assertTrue("State matches", AnalogIO.UNKNOWN == _expression.getState());
        jmri.util.JUnitAppender.assertWarnMessage("Unexpected call to getState in AbstractAnalogExpression.");
        _expression.setState(AnalogIO.INCONSISTENT);
        Assert.assertTrue("State matches", AnalogIO.INCONSISTENT == _expression.getState());
        jmri.util.JUnitAppender.assertWarnMessage("Unexpected call to getState in AbstractAnalogExpression.");
    }
    
    @Test
    public void testEnableAndEvaluate() {
        AnalogExpressionBean _expression = (AnalogExpressionBean)_baseMaleSocket;
        Assert.assertTrue("male socket is enabled", _baseMaleSocket.isEnabled());
        Assert.assertNotEquals("Double don't match", 0.0, _expression.evaluate());
        _baseMaleSocket.setEnabled(false);
        Assert.assertFalse("male socket is disabled", _baseMaleSocket.isEnabled());
        Assert.assertEquals("Double match", 0.0, _expression.evaluate(), 0);
        _baseMaleSocket.setEnabled(true);
        Assert.assertTrue("male socket is enabled", _baseMaleSocket.isEnabled());
        Assert.assertNotEquals("Double don't match", 0.0, _expression.evaluate());
    }
    
    @Test
    public void testDebugConfig() {
        double value1 = 88.99;
        double value2 = 99.88;
        AnalogExpressionBean _expression = (AnalogExpressionBean)_baseMaleSocket;
        Assert.assertNotEquals("Double don't match", value1, _expression.evaluate());
        Assert.assertNotEquals("Double don't match", value2, _expression.evaluate());
        AnalogExpressionDebugConfig debugConfig = new AnalogExpressionDebugConfig();
        debugConfig._forceResult = true;
        debugConfig._result = value1;
        _baseMaleSocket.setDebugConfig(debugConfig);
        Assert.assertEquals("Double match", value1, _expression.evaluate(), 0);
        debugConfig._result = value2;
        Assert.assertEquals("Double match", value2, _expression.evaluate(), 0);
        debugConfig._forceResult = false;
        Assert.assertNotEquals("Double don't match", value1, _expression.evaluate());
        Assert.assertNotEquals("Double don't match", value2, _expression.evaluate());
    }
    
    @Test
    public void testChildAndChildCount() {
        Assert.assertEquals("childCount is equal", _base.getChildCount(), _baseMaleSocket.getChildCount());
        for (int i=0; i < _base.getChildCount(); i++) {
            Assert.assertTrue("child is equal", _base.getChild(i) == _baseMaleSocket.getChild(i));
        }
    }
    
    @Test
    public void testBeanType() {
        Assert.assertEquals("childCount is equal",
                ((NamedBean)_base).getBeanType(),
                ((NamedBean)_baseMaleSocket).getBeanType());
    }
    
    @Test
    public void testDescribeState() {
        Assert.assertEquals("description matches",
                "Unknown",
                ((NamedBean)_baseMaleSocket).describeState(NamedBean.UNKNOWN));
    }
    
}
