package cotton.test.experimental;

import cotton.internalrouting.InternalRoutingNetwork;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author magnus
 */
public class NetworkInSelector implements Runnable {

    private int port;
    private NetPacketDataConverter dataConverter;
    private ServerSocketChannel serverChannel = null;
    private ServerSocket serverSocket = null;
    private Selector selector = null;
    private AtomicInteger running = new AtomicInteger(0);

    public NetworkInSelector(int port) throws IOException {
        this.port = port;
        setup();
    }

    public NetworkInSelector(int port, NetPacketDataConverter dataConverter) throws IOException {
        this.port = port;
        this.dataConverter = dataConverter;
        setup();
    }

    public void setDataOutConverter(NetPacketDataConverter dataConverter) {
        this.dataConverter = dataConverter;
    }

    private void setup() throws IOException {
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverSocket = this.serverChannel.socket();
        InetSocketAddress addr = new InetSocketAddress(Inet4Address.getLocalHost(), this.port);
        System.out.println("Bind port: " + addr.toString());
        this.serverSocket.bind(addr);
        this.selector = Selector.open();
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        running.set(1);
    }

    public void shutdown() {
        running.set(0);
        try {
            this.serverSocket.close();
            this.selector.close();
            this.serverChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(NetworkInSelector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void newReadSocket(Socket socket) throws IOException {
        SocketChannel channel = socket.getChannel();
        channel.configureBlocking(false);
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    private boolean processInData(SocketChannel channel) throws IOException {
        
        return dataConverter.proccessIncoming(channel);
    }

    private void channelReading(SelectionKey key) {
        SocketChannel channel = null;
        try {
            channel = (SocketChannel) key.channel();
            boolean success = processInData(channel);
            if (!success) {
                key.cancel();
                try {
                    Socket socket = channel.socket();
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("NetworkInSelector:channelReading: failed socket close");
                }
            }
        } catch (IOException ex) {
            key.cancel();
            try {
                channel.close();
            } catch (IOException exClose) {
                System.out.println("NetworkInSelector:incoming:Failed to close channel");
            }
        }
    }

    private void incoming() throws IOException {
        Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
        for (SelectionKey key : selectedKeys) {
            if (this.serverSocket.isClosed()) {
                return;
            }
            int ops = key.readyOps();
            if ((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                Socket socket = null;
                try {
                    socket = this.serverSocket.accept();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Logger.getLogger(NetworkInSelector.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }
                newReadSocket(socket);
            } else if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                channelReading(key);
            }
        }
        selectedKeys.clear();
    }

    @Override
    public void run() {
        if (this.dataConverter == null) {
            throw new NullPointerException("NetworkInSelector:run:dataConverter not set null");
        }
        try {
            while (running.get() == 1) {
                int incoming = 0;

                incoming = this.selector.select();

                if (incoming == 0) {
                    continue;
                }

                incoming();
            }
        } catch (ClosedSelectorException ex) {
            Logger.getLogger(NetworkInSelector.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NetworkInSelector.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                this.serverSocket.close();
                this.selector.close();
                this.serverChannel.close();
            } catch (IOException ex) {
                Logger.getLogger(NetworkInSelector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
