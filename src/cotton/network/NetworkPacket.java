package cotton.network;

import java.io.Serializable;

/**
 * The <code>NetworkPacket</code> wraps the data together with needed information 
 * to direct the packet.
 *
 * @author tony
 * @author Magnus
 */
public class NetworkPacket implements Serializable{
    private static final long serialVersionUID = 1L;
    private byte[] data;
    private Origin origin;
    private ServiceChain path;
    private PathType pt;
    private boolean keepAlive;

    /**
     * Constructs a <code>NetworkPacket</code> <strong>without</strong> the 
     * <code>keepAlive</close> flag.
     * 
     * @param data the data to send over the network.
     * @param path the services to be performed.
     * @param origin the service request origin.
     * @param pt the type of packet.
     */
    private NetworkPacket(byte[] data, ServiceChain path, Origin origin, PathType pt) {
        this.data = data;
        this.origin = origin;
        this.path = path;
        if(this.path == null) {
            this.path = new DummyServiceChain();
        }
        this.pt = pt;
        this.keepAlive = false;
    }

    /**
     * Constructs a <code>NetworkPacket</code> <strong>with</strong> the 
     * <code>keepAlive</close> flag.
     * 
     * @param data the data to send over the network.
     * @param path the services to be performed.
     * @param origin the service request origin.
     * @param pt the type of packet.
     * @param keepAlive <code>true</code> if the packet should be <code>keepAlive</code>
     */
    private NetworkPacket(byte[] data, ServiceChain path, Origin origin, PathType pt, boolean keepAlive) {
        this.data = data;
        this.origin = origin;
        this.path = path;
        this.pt = pt;
        this.keepAlive = keepAlive;
    }

    /**
     * Constructs a <code>NetworkPacket</code> <strong>with</strong> the 
     * <code>builder</close>.
     * 
     * @param builder the <code>NetworkPacket</code> builder.
     */
    private NetworkPacket(NetworkPacketBuilder builder){
        this.data = builder.data;
        this.origin = builder.origin;
        this.path = builder.path;
        this.pt = builder.pt;
        this.keepAlive = builder.keepAlive;
    }

    /**
     * Returns the <code>NetworkPacket</code> path in the network.
     * 
     * @return the <code>NetworkPacket</code> path.
     */
    public ServiceChain getPath(){
        return path;
    }

    /**
     * Returns the origin of the <code>NetworkPacket</code>.
     * 
     * @return the <code>NetworkPacket</code> origin.
     */
    public Origin getOrigin(){
        return origin;
    }

    /**
     * Returns the <code>NetworkPacket</code> type.
     * 
     * @return 
     */
    public PathType getType(){
        return pt;
    }

    /**
     * Returns the <code>NetworkPacket</code> data as a byte array.
     * 
     * @return the <code>NetworkPacket</code> data.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns whether it is a <code>keepAlive</code> packet or not.
     * 
     * @return <code>true</code> if its <code>keepAlive</code>.
     */
    public boolean keepAlive(){
        return keepAlive;
    }

    /**
     * Removes the data in the <code>NetworkPacket</code> by setting it to 
     * <code>null</code>.
     */
    public void removeData(){
        data = null;
    }

    /**
     * Returns the <code>NetworkPacket</code> builder.
     * 
     * @return the <code>NetworkPacket</code> builder.
     */
    public static NetworkPacketBuilder newBuilder(){
        return new NetworkPacketBuilder();
    }

    /**
     * An <code>NetworkPacket</code> builder to construct a <code>NetworkPacket</code> 
     * in separate steps.
     */
    public static class NetworkPacketBuilder{
        private byte[] data;
        private Origin origin;
        private ServiceChain path;
        private PathType pt;
        private boolean keepAlive;

        /**
         * Constructs an empty <code>NetworkPacketBuilder</code>.
         */
        public NetworkPacketBuilder(){
            data = null;
            origin = null;
            path = null;
            pt = null;
            keepAlive = false;
        }

        /**
         * Sets the data and returns <code>this</code> to allow recursive 
         * function calls.
         * 
         * @param data the <code>NetworkPacket</code> data.
         * @return <code>this</code>.
         */
        public NetworkPacketBuilder setData(byte[] data){
            this.data = data;
            return this;
        }

        /**
         * Sets the origin and returns <code>this</code> to allow recursive 
         * function calls.
         * 
         * @param origin the <code>NetworkPacket</code> origin.
         * @return <code>this</code>.
         */
        public NetworkPacketBuilder setOrigin(Origin origin){
            this.origin = origin;
            return this;
        }

        /**
         * Sets the <code>ServiceChain</code> and returns <code>this</code> to allow 
         * recursive function calls.
         * 
         * @param path the <code>NetworkPacket ServiceChain</code>.
         * @return <code>this</code>.
         */
        public NetworkPacketBuilder setPath(ServiceChain path){
            this.path = path;
            return this;
        }

        /**
         * Sets the <code>PathType</code> and returns <code>this</code> to allow 
         * recursive function calls.
         * 
         * @param pt the <code>NetworkPacket PathType</code>.
         * @return <code>this</code>.
         */
        public NetworkPacketBuilder setPathType(PathType pt){
            this.pt = pt;
            return this;
        }

        /**
         * Sets the <code>keepAlive</code> flag and returns <code>this</code> to allow 
         * recursive function calls.
         * 
         * @param keepAlive the <code>NetworkPacket keepAlive</code> flag.
         * @return <code>this</code>.
         */
        public NetworkPacketBuilder setKeepAlive(boolean keepAlive){
            this.keepAlive = keepAlive;
            return this;
        }

        /**
         * Builds a <code>NetworkPacket</code> based on the set values and returns 
         * it.
         * 
         * @return the built <code>NetworkPacket</code>.
         */
        public NetworkPacket build(){
            return new NetworkPacket(this);
        }
    }
}
