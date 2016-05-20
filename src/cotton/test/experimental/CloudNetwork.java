
package cotton.test.experimental;

import cotton.configuration.NetworkConfigurator;
import cotton.internalrouting.InternalRoutingNetwork;
import cotton.network.NetworkHandler;
import cotton.network.NetworkPacket;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author magnus
 */
public class CloudNetwork implements NetworkHandler {

    private final SocketAddress localSocketAddress;
    private InternalRoutingNetwork internalRouting = null;
    private ExecutorService threadPool;
    private NetPacketDataConverter dataConverter = null;
    private ConcurrentHashMap<InetSocketAddress, NetworkOut> openOutput;
    private NetworkInSelector netSelector = null;
    private final int localPort;
    private final AtomicInteger running = new AtomicInteger(0);
    
    public CloudNetwork(NetworkConfigurator config) {
        this.localSocketAddress = config.getAddress();
        this.localPort = config.getAddress().getPort();
        initCoreStructures();
    }
    
    public CloudNetwork(int port) throws UnknownHostException {
        this.localSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        this.localPort = port;
        initCoreStructures();
    }

    public CloudNetwork() throws UnknownHostException {
        int port = new Random().nextInt(20000) + 3000;
        this.localSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(),port);
        this.localPort = port;
        initCoreStructures();
    }
    
    private void initCoreStructures() {
        this.threadPool = Executors.newCachedThreadPool();
        this.openOutput = new ConcurrentHashMap<InetSocketAddress, NetworkOut>();
        try {
            this.netSelector = new NetworkInSelector(this.localPort);
        } catch (IOException ex) {
            Logger.getLogger(CloudNetwork.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }
    
    private void setup() throws IOException {
        if(this.internalRouting == null){
            throw new NullPointerException("InternalRoutingNetwork points to null");
        }
        if(this.dataConverter == null){
            this.dataConverter = new NetPacketDataConverter(internalRouting);
        }
        if(this.netSelector == null) {
            this.netSelector = new NetworkInSelector(this.localPort,this.dataConverter);
        }
        this.netSelector.setDataOutConverter(this.dataConverter);
        this.running.set(1);
    }
    private void start() throws IOException {
        //setup();        
        this.threadPool.execute(this.netSelector);
    }

    // tmp for debugging
    private AtomicInteger createCount = new AtomicInteger(0);
    private void startNewNetOut(NetworkPacket netPacket, InetSocketAddress dest) throws IOException {
        NetworkOut out = new NetworkOut(dest, this.threadPool);
        out.bufferData(netPacket);
        if (!out.sendData()) {
            throw new IOException("Failed startNewNetOut  send data");
        }
        NetworkOut p = this.openOutput.putIfAbsent(dest, out);
        if (p != null) {
            out.close();
        }
        int count = createCount.incrementAndGet();
        System.out.println("createConnection count: " + count);
    }

    private void sendNetOut(NetworkPacket netPacket, SocketAddress dest) throws IOException {
        InetSocketAddress addr = (InetSocketAddress)dest;
        NetworkOut out = this.openOutput.get(addr);
        if (out == null) {
            startNewNetOut(netPacket, addr);
            return;
        }
        boolean success = out.bufferData(netPacket) && out.sendData();
        if (!success) {
            this.openOutput.remove((InetSocketAddress)dest, out);
            startNewNetOut(netPacket, addr);
        }
    }
    
    @Override
    public void send(NetworkPacket netPacket, SocketAddress dest) throws IOException {
        if(running.get() == 0)
            return;
        sendNetOut(netPacket,dest);
    }

    @Override
    public void sendKeepAlive(NetworkPacket netPacket, SocketAddress dest) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SocketAddress getLocalAddress() {
        return this.localSocketAddress;
    }

    @Override
    public void setInternalRouting(InternalRoutingNetwork internal) {
        if(internal != null){
            this.internalRouting = internal;
            this.dataConverter = new NetPacketDataConverter(internal);
            try {
                setup();
            } catch (IOException ex) {
                Logger.getLogger(CloudNetwork.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        } else
            throw new NullPointerException("InternalRoutingNetwork points to null");
    }

    @Override
    public void stop() {
        this.running.set(0);    // stop all socket creation and send operations
        for (Map.Entry<InetSocketAddress, NetworkOut> entry : this.openOutput.entrySet()) {
            entry.getValue().close();
        }
        this.netSelector.shutdown();
        this.threadPool.shutdown();
    }

    @Override
    public void run() {
        try {
            start();    // manages its own threads so just start those
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(CloudNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
