package org.coderthoughts.servicejockey;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;

public class Activator implements BundleActivator {
	private ServiceListener listener;
    private ServiceRegistration ereg;
    private ServiceRegistration freg;

    public void start(BundleContext context) throws Exception {
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();

        EventHook eh = new HidingEventHook(context, shc);
        ereg = context.registerService(EventHook.class.getName(), eh, new Hashtable<String, Object>());
        
        FindHook fh = new HidingFindHook(context, shc);
        freg = context.registerService(FindHook.class.getName(), fh, new Hashtable<String, Object>());
        
        listener = new ServiceJockeyListener(context, shc);
        context.addServiceListener(listener);
	}

	public void stop(BundleContext context) throws Exception {
	    context.removeServiceListener(listener);
	    freg.unregister();
	    ereg.unregister();
	}
}
