package main.java.cotton.mockup;

/**
 * Created by Magnus on 2016-04-07.
 */
public interface ServiceChain {
    public String getNextServiceName();    // temporary dummy interface get service name
    public String getCurrentServiceName(); // temporary dummy interface get service name
    public boolean addService(String name); // add temporary service name 
}
