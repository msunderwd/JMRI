/**
 * 
 */
package jmri.configurexml;

import org.jdom.Element;

import com.sun.java.util.collections.List;

import jmri.InstanceManager;
import jmri.TurnoutManager;
import jmri.TurnoutOperation;
import jmri.TurnoutOperationManager;
import jmri.configurexml.XmlAdapter;
import jmri.configurexml.turnoutoperations.TurnoutOperationXml;

/**
 * @author John Harper
 *
 */
public class TurnoutOperationManagerXml implements XmlAdapter {

    public TurnoutOperationManagerXml() {
    }

    public void setStoreElementClass(Element elem) {
        elem.addAttribute("class",getClass().getName());
    }

    public void load(Element element, Object o) {
        log.error("Invalid method called");
    }

    public void load(Element operationsElement) {
    	TurnoutOperationManager manager = TurnoutOperationManager.getInstance();
        if (operationsElement.getAttribute("automate") != null) {
        	try {
            	manager.setDoOperations(operationsElement.getAttribute("automate").getValue().equals("true"));        		
        	} catch(NumberFormatException ex) { };
        }
    	List operationsList = operationsElement.getChildren("operation");
    	if (log.isDebugEnabled()) log.debug("Found "+operationsList.size()+" operations");
    	for (int i=0; i<operationsList.size(); i++) {
    		Element thisOpElement = (Element)operationsList.get(i);
    		String className = thisOpElement.getAttributeValue("class");
    		if (className==null) {
    			log.error("class name missing in turnout operation \""+thisOpElement+"\"");
    		} else {
    			try {
    				Class adapterClass = Class.forName(className);
    				if (adapterClass != null) {
    					TurnoutOperationXml adapter = (TurnoutOperationXml)adapterClass.newInstance();
    					adapter.load(thisOpElement);
    				}
    			} catch (Exception e) {
    				log.error("failed to find or run adapter class for "+className);
    			}
    		}
    	}
    }

    public Element store(Object o) {
    	Element elem = new Element("operations");
    	if (o instanceof TurnoutOperationManager) {
    		TurnoutOperationManager manager = (TurnoutOperationManager)o;
    		elem.addAttribute("automate", String.valueOf(manager.getDoOperations()));
    		TurnoutOperation[] operations = manager.getTurnoutOperations();
    		for (int i=0; i<operations.length; ++i) {
    			TurnoutOperation op = operations[i];
    			TurnoutOperationXml adapter = TurnoutOperationXml.getAdapter(op);
    			if (adapter != null) {
    				Element opElem = adapter.store(op);
    				if (opElem != null) {
    					elem.addContent(opElem);
    				}
    			}
    		}
    	}
    	return elem;
    }

    static org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(TurnoutOperationManagerXml.class.getName());
}
