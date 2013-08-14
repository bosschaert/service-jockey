package org.coderthoughts.servicejockey;

public class RestrictRule extends Rule {
    String extraFilter;

    public String getExtraFilter() {
        return extraFilter;
    }

    public void setExtraFilter(String val) {
        extraFilter = val;
    }
}
