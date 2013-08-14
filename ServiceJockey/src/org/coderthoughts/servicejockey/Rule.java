package org.coderthoughts.servicejockey;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public abstract class Rule {
    String bsn;
    Version bundleVersion;
    String serviceFilter;

    public String getBSN() {
        return bsn;
    }
    
    public Version getBundleVersion() {
        return bundleVersion;
    }

    public String getServiceFilter() {
        return serviceFilter;
    }

    public void setBSN(String val) {
        bsn = val;
    }
    
    public void setBundleVersion(Version ver) {
        bundleVersion = ver;
    }
    
    public void setServiceFilter(String val) {
        serviceFilter = val;
    }

    public boolean matches(ServiceReference sr, BundleContext bc) throws Exception {
        if (getBSN() != null) {
            if (!bc.getBundle().getSymbolicName().matches(getBSN())) {
                return false;
            }
        }
        
        if (getBundleVersion() != null) {
            if (!bc.getBundle().getVersion().equals(getBundleVersion())) {
                return false;
            }
        }
       
        if (getServiceFilter() != null) {
            Filter f = bc.createFilter(getServiceFilter());
            return f.match(sr);
        } else {
            return false;
        }
    }
}
