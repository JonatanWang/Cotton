package cotton.services;

/**
 *
 *
 * @author Tony Tran
 **/
public interface ServiceBuffer{

    public ServicePacket nextPacket();

    public boolean add(ServicePacket servicePacket);

}
