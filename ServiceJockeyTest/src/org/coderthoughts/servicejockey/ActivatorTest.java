package org.coderthoughts.servicejockey;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;

public class ActivatorTest extends TestCase {
    public void testActivator() throws Exception {
        ServiceRegistration sreg = mock(ServiceRegistration.class);
        BundleContext bc = mock(BundleContext.class);
        when(bc.registerService(anyString(), anyObject(), any(Dictionary.class))).thenReturn(sreg); 
        
        Activator a = new Activator();
        a.start(bc);
        
        verify(bc).registerService(eq(EventHook.class.getName()), isA(HidingEventHook.class), any(Dictionary.class));
        verify(bc).registerService(eq(FindHook.class.getName()), isA(HidingFindHook.class), any(Dictionary.class));
        verify(bc).addServiceListener(isA(ServiceJockeyListener.class));
        
        verify(bc, never()).removeServiceListener((ServiceListener) anyObject());
        verify(sreg, never()).unregister();
        a.stop(bc);
        verify(bc).removeServiceListener(isA(ServiceJockeyListener.class));
        verify(sreg, times(2)).unregister();
    }
}
