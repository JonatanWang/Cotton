package main.java.cotton.mockup;

/**
 *
 *Created by Tony on 2016-04-11 
 **/
public interface ServiceBuffer{
    public ServicePacket nextPacket();
    public boolean add(ServicePacket servicePacket);
}
