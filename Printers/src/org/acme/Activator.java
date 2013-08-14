package org.acme;

import java.util.Arrays;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private ServiceRegistration reg1;
    private ServiceRegistration reg2;
    private ServiceTracker st;

    public void start(BundleContext context) throws Exception {
	    Printer p1 = new PrinterImpl();
	    Hashtable<String, Object> props1 = new Hashtable<String, Object>();
	    props1.put("name", "p1");
	    props1.put("location", "b283");
	    props1.put("capabilities", new String [] {"Double-sided"});
	    props1.put("paper-size", new String [] {"A3", "A4"});
        reg1 = context.registerService(Printer.class.getName(), p1, props1);
	    
        Printer p2 = new PrinterImpl();
        Hashtable<String, Object> props2 = new Hashtable<String, Object>();
        props2.put("name", "p7");
        props2.put("location", "a12");
        props2.put("capabilities", new String [] {"Colour", "Staple"});
        props2.put("paper-size", new String [] {"A4", "Letter"});
        reg2 = context.registerService(Printer.class.getName(), p2, props2);
        
        // Filter filter = context.createFilter("(&(objectClass=testbundle.Printer)(paper-size=A4))");        
        Filter filter = context.createFilter("(&(objectClass=org.acme.Printer)(paper-size=A4))");        
        st = new ServiceTracker(context, filter, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                // print out some information of the printer / use the printer
                System.out.println("Added Printer:");
                for (String key : reference.getPropertyKeys()) {
                    Object val = reference.getProperty(key);
                    if (val.getClass().isArray()) {
                        val = Arrays.asList((Object []) val);
                    }
                    System.out.println(key + ": " + val);
                }
                System.out.println();
                
                Object p = super.addingService(reference);
                if (p instanceof Printer) {
                    ((Printer) p).print("Hello");
                }
                return p;
            }            
        };
        st.open();
	}
	
	public void stop(BundleContext context) throws Exception {
	    st.close();
	    reg1.unregister();
	    reg2.unregister();
	}
}
