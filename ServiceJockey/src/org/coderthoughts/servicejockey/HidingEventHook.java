package org.coderthoughts.servicejockey;

import java.util.Collection;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventHook;

public class HidingEventHook implements EventHook {
    private final ServiceHandlerCatalog catalog;
    private BundleContext myBC;    

    public HidingEventHook(BundleContext bc, ServiceHandlerCatalog shc) {
        myBC = bc;
        catalog = shc;
    }

    @SuppressWarnings("unchecked")
    public void event(ServiceEvent event, Collection contexts) {
        ServiceReference sr = event.getServiceReference();
        if (catalog.getProxiedServices().contains(sr) || catalog.willProxy(sr)) {
            for (Iterator i = contexts.iterator(); i.hasNext(); ) {
                BundleContext bc = (BundleContext) i.next();
                
                if (myBC.equals(bc) || bc.getBundle().getBundleId() == 0) {
                    // don't hide anything from me nor the system bundle
                    continue;
                }                
                // hide because it's proxied
                i.remove();
            }
            return;
        }
        
        // Maybe there are additional constraints...
        for (Iterator i = contexts.iterator(); i.hasNext(); ) {
            BundleContext bc = (BundleContext) i.next();
            if (myBC.equals(bc) || bc.getBundle().getBundleId() == 0) {
                // don't hide anything from me nor the system bundle
                continue;
            }
            
            if (catalog.hideService(sr, bc)) {
                // hide because there are additional unsatisfied constraints
                i.remove();
            }            
        }
    }        
}
