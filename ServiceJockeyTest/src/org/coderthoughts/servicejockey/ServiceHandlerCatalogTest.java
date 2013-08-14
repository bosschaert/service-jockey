package org.coderthoughts.servicejockey;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class ServiceHandlerCatalogTest extends TestCase {
    public void testReadConfigFile() throws Exception {
        URL url = getClass().getResource("data/sj1.xml");
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        shc.addDefinition(url);
        assertEquals(1, shc.restrictRules.size());
        
        RestrictRule rr = shc.restrictRules.iterator().next();
        assertEquals(".*", rr.getBSN());
        assertNull(rr.getBundleVersion());
        assertEquals("(objectClass=*MyService)", rr.getServiceFilter());
        assertEquals("(!(osgi.remote=true))", rr.getExtraFilter());
        
        assertEquals(2, shc.proxyRules.size());
        Rule r1 = shc.proxyRules.get(0);
        assertEquals("org.blah.boo", r1.getBSN());
        assertEquals(new Version("1.2.3"), r1.getBundleVersion());
        Rule r2 = shc.proxyRules.get(1);
        assertNull(r2.getBSN());
        assertNull(r2.getBundleVersion());
    }
        
    @SuppressWarnings("unchecked")
    public void testProxyService() throws Exception {
        Filter filter = mock(Filter.class);
        
        BundleContext bc = mock(BundleContext.class);
        when(bc.createFilter("(mykey=myvalue)")).thenReturn(filter);

        Bundle b = mock(Bundle.class);
        when(b.getBundleContext()).thenReturn(bc);
        
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        ProxyRule r = new ProxyRule();
        r.setServiceFilter("(mykey=myvalue)");
        r.addAddProperty("x", "y");
        shc.proxyRules.add(r);
        
        final ServiceRegistration reg = mock(ServiceRegistration.class);

        Object svc = "hi";
        ServiceReference sr = mock(ServiceReference.class);
        when(sr.getBundle()).thenReturn(b);
        when(sr.getPropertyKeys()).thenReturn(new String [] {"mykey", "objectClass"});
        when(sr.getProperty("mykey")).thenReturn("myvalue");
        when(sr.getProperty("objectClass")).thenReturn(new String [] {String.class.getName()});
        when(filter.match(sr)).thenReturn(true);
        when(bc.getService(sr)).thenReturn(svc);                

        when(bc.registerService((String[]) anyObject(), anyObject(), (Dictionary) anyObject()))
            .thenAnswer(new Answer<ServiceRegistration>() {
                public ServiceRegistration answer(InvocationOnMock invocation) throws Throwable {
                    assertTrue(Arrays.deepEquals(new String [] {String.class.getName()}, 
                            (String []) invocation.getArguments()[0]));

                    Hashtable<String, Object> actual = 
                        (Hashtable<String, Object>) invocation.getArguments()[2];
                    Hashtable<String, Object> expected = new Hashtable<String, Object>();
                    expected.put("mykey", "myvalue");
                    expected.put("objectClass", actual.get("objectClass"));
                    expected.put("x", "y");
                    expected.put(".ServiceJockey", "Proxied");
                    assertEquals(expected, actual);
                    return reg;
                }
            });
        
        ServiceReference sr2 = mock(ServiceReference.class);
        when(sr2.getBundle()).thenReturn(b);        
        
        verify(bc, never()).registerService((String [])anyObject(), anyObject(), (Dictionary) anyObject());
        assertTrue(shc.willProxy(sr));
        assertFalse(shc.willProxy(sr2));
        verify(bc, never()).registerService((String [])anyObject(), anyObject(), (Dictionary) anyObject());        
        shc.serviceRegistered(sr);

        // trigger the event again
        shc.serviceRegistered(sr); // should have no effect
        verify(bc, times(1)).registerService((String [])anyObject(), anyObject(), (Dictionary) anyObject());
        
        verify(reg, never()).unregister();
        shc.serviceUnregistering(sr);
        verify(reg, times(1)).unregister();
        shc.serviceUnregistering(sr); // should have no effect
        verify(reg, times(1)).unregister();
    }
    
    public void testHideService() throws Exception {
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setBSN(".*Foo");
        r.setServiceFilter("(objectClass=java.lang.String)");
        r.setExtraFilter("(test=blah)");
        shc.restrictRules.add(r);
        
        ServiceReference sr = mock(ServiceReference.class);
        BundleContext bc = mock(BundleContext.class);
        Bundle b = mock(Bundle.class);
        when(b.getSymbolicName()).thenReturn("TestFoo");
        when(bc.getBundle()).thenReturn(b);
        
        Filter filter = mock(Filter.class);
        when(filter.match(sr)).thenReturn(false);
        when(bc.createFilter("(test=blah)")).thenReturn(filter);
        
        Filter filter2 = mock(Filter.class);
        when(filter2.match(sr)).thenReturn(true);
        when(bc.createFilter("(objectClass=java.lang.String)")).thenReturn(filter2);
        
        assertTrue(shc.hideService(sr, bc));        
    }
    
    public void testDontHideService() throws Exception {
        ServiceHandlerCatalog shc = new ServiceHandlerCatalog();
        RestrictRule r = new RestrictRule();
        r.setServiceFilter("(objectClass=java.lang.String)");
        r.setExtraFilter("(test=blah)");
        shc.restrictRules.add(r);
        
        ServiceReference sr = mock(ServiceReference.class);
        
        BundleContext bc = mock(BundleContext.class);
        Filter filter = mock(Filter.class);
        when(filter.match(sr)).thenReturn(true);
        when(bc.createFilter("(test=blah)")).thenReturn(filter);
        
        Filter filter2 = mock(Filter.class);
        when(filter2.match(sr)).thenReturn(true);
        when(bc.createFilter("(objectClass=java.lang.String)")).thenReturn(filter2);

        assertFalse(shc.hideService(sr, bc));        
    }
}
