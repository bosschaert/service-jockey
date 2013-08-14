package org.coderthoughts.servicejockey;

import java.util.Collection;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.FindHook;

public class HidingFindHook implements FindHook {
    private final BundleContext myBC;
    private final ServiceHandlerCatalog catalog;
    
    HidingFindHook(BundleContext bc, ServiceHandlerCatalog shc) {
        myBC = bc;
        catalog = shc;
    }
    
    @SuppressWarnings("unchecked")
    public void find(BundleContext context, String name, String filter,
            boolean allServices, Collection references) {
        if (myBC.equals(context) || context.getBundle().getBundleId() == 0) {
            // don't hide anything from myself nor the system bundle
            return;
        }
        
        // Remove all services that have been proxied
        references.removeAll(catalog.getProxiedServices());
        
        for (Iterator i = references.iterator(); i.hasNext(); ) {
            ServiceReference sr = (ServiceReference) i.next();
            if (catalog.hideService(sr, context)) {
                i.remove();
            }
        }
    }
}
