package org.coderthoughts.servicejockey;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class ServiceHandlerCatalog {    
    private static final String SERVICE_JOCKEY_PROP = ".ServiceJockey";
    private static final Object SERVICE_JOCKEY_PROP_VAL = "Proxied";

    private final DocumentBuilder parser;
    
    List<RestrictRule> restrictRules = new ArrayList<RestrictRule>();
    List<ProxyRule> proxyRules = new ArrayList<ProxyRule>();
    Map<ServiceReference, ServiceRegistration> proxies = 
        new ConcurrentHashMap<ServiceReference, ServiceRegistration>();

    public ServiceHandlerCatalog() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        parser = factory.newDocumentBuilder();        
    }
    
    /** Returns a collection of all the services that are proxied. The collection is live and 
     * cannot be modified by the client.
     */
    public Collection<ServiceReference> getProxiedServices() {
        return Collections.unmodifiableSet(proxies.keySet());
    }

    private synchronized void parseStream(InputStream is) throws Exception {
        Document doc = parser.parse(is);
        
        NodeList nodes = doc.getDocumentElement().getChildNodes();
        for (int i=0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ("restrict-visibility".equals(node.getNodeName())) {
                addRestrictRules(node);
            } else if ("proxy-registration".equals(node.getNodeName())) {
                addProxyRules(node);
            }
        }
    }

    private void addRestrictRules(Node node) throws Exception {
        restrictRules.addAll(readRules(node, RestrictRule.class));
    }

    private void addProxyRules(Node node) throws Exception {
        proxyRules.addAll(readRules(node, ProxyRule.class));
    }
    
    private <T extends Rule> List<T> readRules(Node node, Class<T> ruleClass) throws Exception {
        List<T> rules = new ArrayList<T>();
        NodeList nl = node.getChildNodes();
        for (int i=0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (!(n instanceof Element)) {
                continue;
            }
            
            Element e = (Element) n;
            if ("rule".equals(n.getNodeName())) {
                T r = ruleClass.newInstance();
                fillRule(e, r);
                rules.add(r);
            }
        }
        return rules;
    }

    private void fillRule(Element e, Rule r) {
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node c = nl.item(i);
            if (!(c instanceof Element)) {
                continue;
            }
            Element ce = (Element) c;
            String text = ce.getTextContent();
            
            if ("bsn".equals(ce.getNodeName())) {
                r.setBSN(text);
            } else if ("bver".equals(ce.getNodeName())) {
                r.setBundleVersion(new Version(text));
            } else if ("service-filter".equals(ce.getNodeName())) {
                r.setServiceFilter(text);
            } else if ("add-filter".equals(ce.getNodeName())) {
                if (r instanceof RestrictRule) {
                    ((RestrictRule) r).setExtraFilter(text);                    
                }
            } else if ("add-property".equals(ce.getNodeName())) {
                if (r instanceof ProxyRule) {
                    ((ProxyRule) r).addAddProperty(ce.getAttribute("key"), text);
                }
            }
        }
    }

    public void serviceRegistered(ServiceReference sr) throws Exception {
        ProxyRule r = getProxyRule(sr);
        if (r != null) {
            proxyService(sr, r);
        }
    }

    private ProxyRule getProxyRule(ServiceReference sr) throws Exception {
        if (sr.getProperty(SERVICE_JOCKEY_PROP) != null) {
            // Ignore proxies created by me
            return null;
        }
        
        for (ProxyRule pr : proxyRules) {
            if (pr.matches(sr, sr.getBundle().getBundleContext())) {
                return pr;
            }
        }
        return null;
    }
    
    public boolean willProxy(ServiceReference sr) {
        try {
            return getProxyRule(sr) != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean hideService(ServiceReference sr, BundleContext requestingBC) {
        try {
            for (RestrictRule r : restrictRules) {
                if (r.matches(sr, requestingBC)) {
                    String s = r.getExtraFilter();
                    Filter f = requestingBC.createFilter(s);
                    return !f.match(sr);
                }
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void serviceUnregistering(ServiceReference sr) {
        ServiceRegistration reg = proxies.remove(sr);
        if (reg != null) {
            reg.unregister();
        }
    }    

    private void proxyService(ServiceReference sr, ProxyRule pr) {
        if (proxies.get(sr) != null) {
            // proxied this one already...
            return;
        }
        
        BundleContext ctx = sr.getBundle().getBundleContext();
        Object svc = ctx.getService(sr);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        for (String key : sr.getPropertyKeys()) {
            props.put(key, sr.getProperty(key));
        }
        props.putAll(pr.getAddProperties());
        props.put(SERVICE_JOCKEY_PROP, SERVICE_JOCKEY_PROP_VAL);
        
        proxies.put(sr, ctx.registerService((String []) sr.getProperty(Constants.OBJECTCLASS), svc, props));
    }

    public void addDefinition(URL entry) throws Exception {
        parseStream(entry.openStream());
    }
}
