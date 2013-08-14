package org.coderthoughts.servicejockey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;

public class ServiceJockeyListenerTest extends TestCase {
    public void testServiceJockeyListener() throws Exception {
        ServiceHandlerCatalog shc = mock(ServiceHandlerCatalog.class);
        BundleContext bc = mock(BundleContext.class);
        ServiceJockeyListener sjl = new ServiceJockeyListener(bc, shc);
        assertSame(shc, sjl.shc);
        assertNotNull(sjl.bt);

        Method m = BundleTracker.class.getDeclaredMethod("tracked");
        m.setAccessible(true);
        assertNotNull(m.invoke(sjl.bt));
        
        verify(shc, never()).addDefinition((URL) Mockito.anyObject());
        
        Bundle b = mock(Bundle.class);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("Service-Jockey", "entry");
        when(b.getHeaders()).thenReturn(props);
        when(b.getEntry("/entry")).thenReturn(new URL("http://localhost/entry"));
        sjl.bt.addingBundle(b, null);
        
        verify(shc).addDefinition(new URL("http://localhost/entry"));
        
        sjl.close();
        assertNull("Bundle trakcer should have been closed", m.invoke(sjl.bt));        
    }
    
    public void testServiceRegistered() throws Exception {
        ServiceHandlerCatalog shc = mock(ServiceHandlerCatalog.class);
        BundleContext bc = mock(BundleContext.class);
        ServiceJockeyListener sjl = new ServiceJockeyListener(bc, shc);
        ServiceReference sr = mock(ServiceReference.class);
        ServiceEvent se = new ServiceEvent(ServiceEvent.REGISTERED, sr);
        sjl.serviceChanged(se);
        verify(shc).serviceRegistered(sr);
        verifyNoMoreInteractions(shc);
    }

    public void testServiceUnregisterUNG() throws Exception {
        ServiceHandlerCatalog shc = mock(ServiceHandlerCatalog.class);
        BundleContext bc = mock(BundleContext.class);
        ServiceJockeyListener sjl = new ServiceJockeyListener(bc, shc);
        ServiceReference sr = mock(ServiceReference.class);
        ServiceEvent se = new ServiceEvent(ServiceEvent.UNREGISTERING, sr);
        sjl.serviceChanged(se);
        verify(shc).serviceUnregistering(sr);
        verifyNoMoreInteractions(shc);
    }

    public void testServiceChangedOtherEvents() {
        ServiceHandlerCatalog shc = mock(ServiceHandlerCatalog.class);
        BundleContext bc = mock(BundleContext.class);
        ServiceJockeyListener sjl = new ServiceJockeyListener(bc, shc);
        ServiceReference sr = mock(ServiceReference.class);
        ServiceEvent se = new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, sr);
        sjl.serviceChanged(se);
        verifyZeroInteractions(shc);
    }
}