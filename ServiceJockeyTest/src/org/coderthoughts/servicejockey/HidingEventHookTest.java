package org.coderthoughts.servicejockey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class HidingEventHookTest extends TestCase {
    public void testHidingEventHook() throws Exception {       
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        shc.proxies.put(sr, mock(ServiceRegistration.class));
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(0, ctxs.size());
    }
    
    public void testHidingEventHookWillProxy() throws Exception {       
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        when(b.getBundleContext()).thenReturn(bc);

        final ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        ProxyRule r = new ProxyRule() {
            @Override
            public boolean matches(ServiceReference ref, BundleContext bc) throws Exception {
                return ref == sr;
            }            
        };
        
        shc.proxyRules.add(r);
        
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(0, ctxs.size());
    }

    public void testHidingEventHookWillNotProxy() throws Exception {       
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        when(b.getBundleContext()).thenReturn(bc);

        final ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        ProxyRule r = new ProxyRule() {
            @Override
            public boolean matches(ServiceReference ref, BundleContext bc) throws Exception {
                return false;
            }            
        };
        
        shc.proxyRules.add(r);
        
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(1, ctxs.size());
        assertSame(bc, ctxs.iterator().next());
    }

    public void testHidingEventHookWillNotHideSystemBundle() throws Exception {       
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(0L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        when(b.getBundleContext()).thenReturn(bc);

        final ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        ProxyRule r = new ProxyRule() {
            @Override
            public boolean matches(ServiceReference ref, BundleContext bc) throws Exception {
                return false;
            }            
        };
        
        shc.proxyRules.add(r);
        
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(1, ctxs.size());
        assertSame(bc, ctxs.iterator().next());
    }

    public void testHidingEventHookWillHide() throws Exception {       
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(45L);
        when(b.getSymbolicName()).thenReturn("org.acme.test");
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        when(b.getBundleContext()).thenReturn(bc);
        final ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        
        Filter f = mock(Filter.class);
        when(f.match(sr)).thenReturn(true);
        Filter f2 = mock(Filter.class);
        when(f2.match(sr)).thenReturn(false);
        when(bc.createFilter("(a=b)")).thenReturn(f);
        when(bc.createFilter("(foo=bar)")).thenReturn(f2);

        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setBSN("org.acme.test");
        r.setServiceFilter("(a=b)");
        r.setExtraFilter("(foo=bar)");        
        shc.restrictRules.add(r);
        
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(0, ctxs.size());
    }

    public void testHidingEventHookWillHide2() throws Exception {       
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(45L);
        when(b.getSymbolicName()).thenReturn("org.acme.test");
        when(b.getVersion()).thenReturn(new Version("1.3.2"));
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        when(b.getBundleContext()).thenReturn(bc);
        final ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        
        Filter f = mock(Filter.class);
        when(f.match(sr)).thenReturn(true);
        Filter f2 = mock(Filter.class);
        when(f2.match(sr)).thenReturn(false);
        when(bc.createFilter("(a=b)")).thenReturn(f);
        when(bc.createFilter("(foo=bar)")).thenReturn(f2);

        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setBSN("org(.*)");
        r.setBundleVersion(new Version("1.3.2"));
        r.setServiceFilter("(a=b)");
        r.setExtraFilter("(foo=bar)");        
        shc.restrictRules.add(r);
        
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(0, ctxs.size());
    }

    public void testHidingEventHookWillNotHide2() throws Exception {       
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(45L);
        when(b.getSymbolicName()).thenReturn("org.acme.test");
        when(b.getVersion()).thenReturn(new Version("1.3.4"));
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        when(b.getBundleContext()).thenReturn(bc);
        final ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        
        Filter f = mock(Filter.class);
        when(f.match(sr)).thenReturn(true);
        Filter f2 = mock(Filter.class);
        when(f2.match(sr)).thenReturn(false);
        when(bc.createFilter("(a=b)")).thenReturn(f);
        when(bc.createFilter("(foo=bar)")).thenReturn(f2);

        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setBSN("org(.*)");
        r.setBundleVersion(new Version("1.3.2"));
        r.setServiceFilter("(a=b)");
        r.setExtraFilter("(foo=bar)");        
        shc.restrictRules.add(r);
        
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(1, ctxs.size());
        assertSame(bc, ctxs.iterator().next());
    }

    public void testHidingEventHookWillNotHide() throws Exception {       
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(45L);
        when(b.getSymbolicName()).thenReturn("org.acme.test");
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        when(b.getBundleContext()).thenReturn(bc);
        final ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        
        Filter f = mock(Filter.class);
        when(f.match(sr)).thenReturn(true);
        Filter f2 = mock(Filter.class);
        when(f2.match(sr)).thenReturn(true);
        when(bc.createFilter("(a=b)")).thenReturn(f);
        when(bc.createFilter("(foo=bar)")).thenReturn(f2);

        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setBSN("org.acme.test");
        r.setServiceFilter("(a=b)");
        r.setExtraFilter("(foo=bar)");        
        shc.restrictRules.add(r);
        
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(1, ctxs.size());
        assertSame(bc, ctxs.iterator().next());
    }

    public void testNonHidingEventHookSystemBundle() throws Exception {       
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        shc.proxies.put(sr, mock(ServiceRegistration.class));
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(0L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        HidingEventHook eh = new HidingEventHook(mock(BundleContext.class), shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(1, ctxs.size());
        assertSame(bc, ctxs.iterator().next());
    }
    
    public void testNonHidingEventHookMyself() throws Exception {       
        ServiceReference sr = mock(ServiceReference.class);
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        shc.proxies.put(sr, mock(ServiceRegistration.class));
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(42L);
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(b);
        HidingEventHook eh = new HidingEventHook(bc, shc);
        
        Collection<BundleContext> ctxs = new ArrayList<BundleContext>();
        ctxs.add(bc);
        ServiceEvent ev = new ServiceEvent(0, sr);
        
        assertEquals("Precondition failed", 1, ctxs.size());
        eh.event(ev, ctxs);
        assertEquals(1, ctxs.size());
        assertSame(bc, ctxs.iterator().next());
    }
}
