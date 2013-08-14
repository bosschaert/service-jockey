package org.coderthoughts.servicejockey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class HidingFindHookTest extends TestCase {
    public void testHidingFindHook() throws Exception {
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        shc.proxies.put(sr, mock(ServiceRegistration.class));
        
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        HidingFindHook fh = new HidingFindHook(mock(BundleContext.class), shc);

        List<ServiceReference> refs = new ArrayList<ServiceReference>();
        refs.add(sr);
        fh.find(bc, "somename", null, false, refs);
        assertEquals(0, refs.size());
    }

    public void testNonHidingFindHookMyBundle() throws Exception {
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        shc.proxies.put(sr, mock(ServiceRegistration.class));
        
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        HidingFindHook fh = new HidingFindHook(bc, shc);

        List<ServiceReference> refs = new ArrayList<ServiceReference>();
        refs.add(sr);
        fh.find(bc, "somename", null, false, refs);
        assertEquals(1, refs.size());
    }

    public void testNonHidingFindHookSystemBundle() throws Exception {
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        shc.proxies.put(sr, mock(ServiceRegistration.class));
        
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(0L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        HidingFindHook fh = new HidingFindHook(bc, shc);

        List<ServiceReference> refs = new ArrayList<ServiceReference>();
        refs.add(sr);
        fh.find(bc, "somename", null, false, refs);
        assertEquals(1, refs.size());
        assertSame(sr, refs.iterator().next());
    }

    public void testHidingFindHookThroughAddedCriteria() throws Exception {
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setServiceFilter("(foo=bar)");
        r.setExtraFilter("(a=b)");
        shc.restrictRules.add(r);
        
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        Filter f = mock(Filter.class);
        when(f.match(sr)).thenReturn(true);
        when(bc.createFilter("(foo=bar)")).thenReturn(f);
        Filter f2 = mock(Filter.class);
        when(f2.match(sr)).thenReturn(false);
        when(bc.createFilter("(a=b)")).thenReturn(f2);
        
        HidingFindHook fh = new HidingFindHook(mock(BundleContext.class), shc);

        List<ServiceReference> refs = new ArrayList<ServiceReference>();
        refs.add(sr);
        fh.find(bc, "somename", null, false, refs);
        assertEquals(0, refs.size());
    }    

    public void testNonHidingFindHookThroughAddedCriteria() throws Exception {
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setServiceFilter("(foo=bar)");
        r.setExtraFilter("(a=b)");
        shc.restrictRules.add(r);
        
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        Filter f = mock(Filter.class);
        when(f.match(sr)).thenReturn(true);
        when(bc.createFilter("(foo=bar)")).thenReturn(f);
        Filter f2 = mock(Filter.class);
        when(f2.match(sr)).thenReturn(true);
        when(bc.createFilter("(a=b)")).thenReturn(f2);
        
        HidingFindHook fh = new HidingFindHook(mock(BundleContext.class), shc);

        List<ServiceReference> refs = new ArrayList<ServiceReference>();
        refs.add(sr);
        fh.find(bc, "somename", null, false, refs);
        assertEquals(1, refs.size());
        assertSame(sr, refs.iterator().next());
    }    
}
