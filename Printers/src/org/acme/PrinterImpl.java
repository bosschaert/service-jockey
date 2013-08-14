package org.acme;

public class PrinterImpl implements Printer {
    @Override
    public boolean print(String doc) {
        System.out.println("Printing: " + doc);
        return false;
    }
}
