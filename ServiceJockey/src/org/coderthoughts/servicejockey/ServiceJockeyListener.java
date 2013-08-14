package org.coderthoughts.servicejockey;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.util.tracker.BundleTracker;

public class ServiceJockeyListener implements ServiceListener {
    final ServiceHandlerCatalog shc;
    BundleTracker bt;

    public ServiceJockeyListener(BundleContext context, ServiceHandlerCatalog catalog) {
        shc = catalog;
        
        bt = new BundleTracker(context, Bundle.STARTING | Bundle.ACTIVE, null) {
            @Override
            @SuppressWarnings("unchecked")
            public Object addingBundle(Bundle bundle, BundleEvent event) {
                System.out.println("*** Bundle: " + bundle.getSymbolicName());
                Dictionary props = bundle.getHeaders();
                Object header = props.get("Service-Jockey");
                if (header != null) {
                    try {
                        shc.addDefinition(bundle.getEntry("/" + header));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return super.addingBundle(bundle, event);
            }            
        };
        bt.open();
    }
    
    public void close() {
        bt.close();
    }

    public void serviceChanged(ServiceEvent event) {
        try {
            switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                shc.serviceRegistered(event.getServiceReference());
                break;
            case ServiceEvent.UNREGISTERING:
                shc.serviceUnregistering(event.getServiceReference());
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
