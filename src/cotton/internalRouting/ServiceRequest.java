package cotton.internalRouting;

/**
 *
 * @author tony
 * @author Magnus
 */
public interface ServiceRequest {
    public byte[] getData();
    public void setFailed(byte[] errorMessage);
}
