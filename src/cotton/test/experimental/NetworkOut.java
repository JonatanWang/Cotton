/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.test.experimental;

import cotton.network.NetworkPacket;
import cotton.network.TransportPacket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NetworkOut is used to send data to a destination, this is done in to steps
 * First : call bufferData and send in the data packet Last : call sendData to
 * force it out to the net if needed
 *
 * @author magnus
 */
public class NetworkOut {

    private final SocketAddress destination;
    private final AtomicInteger check = new AtomicInteger(0);
    private Socket socket = null;
    private final ConcurrentLinkedQueue<TransportPacket.Packet> queue = new ConcurrentLinkedQueue<>();
    private final ExecutorService exec;

    /**
     * Creates a output to a specific destination, can then be reused
     *
     * @param destination the destination this network output connection should
     * point
     * @param exec a ExecutorService for spawning new threads when needed
     * @throws IOException if things go bad, or cant reach host
     */
    public NetworkOut(SocketAddress destination, ExecutorService exec) throws IOException {
        this.exec = exec;
        this.destination = destination;
        this.openSocket();
    }

    /**
     * If called this will prevent any more data to be sent,
     *
     * @return what was still in the buffer, (not guaranteed to include late
     * adds )
     */
    public ArrayList<TransportPacket.Packet> close() {
        this.check.set(2); // no new incoming packages,
        ArrayList<TransportPacket.Packet> ret = new ArrayList<TransportPacket.Packet>();
        TransportPacket.Packet pkt = null;
        while ((pkt = this.queue.poll()) != null) {
            ret.add(pkt);
        }
        try {
            this.socket.close();
        } catch (IOException ex) {
            Logger.getLogger(NetworkOut.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    private void openSocket() throws IOException {
        this.socket = new Socket();
        socket.setKeepAlive(true);
        socket.setSoLinger(true, 0);
        socket.setReuseAddress(true);
        //socket.setTcpNoDelay(true);
        this.socket.connect(this.destination);
    }

    /**
     * Creates the TransportPacket and buffer it to make it ready for sending
     *
     * @param packet packet that should be sent
     * @throws IOException
     */
    public boolean bufferData(NetworkPacket packet) throws IOException {
        if (this.check.get() == 2) {
            return false;
        }
        TransportPacket.Packet pkt = NetPacketDataConverter.buildTransportPacket(packet);
        this.queue.add(pkt);
        return true;
    }

    /**
     * This sends the packet
     *
     * @return if false this NetworkOut object has gone bad and needs to be
     * reaped
     */
    public boolean sendData() throws IOException {
        int val = check.getAndSet(1);
        if (val == 1) {
            return true;    // already a thread sending stuff, dont need to do anything
        }
        if (val == 2) {
            return false;
        }
        TransportPacket.Packet packet = null;
        boolean done = false;
        try {
            for (int i = 0; i < 3; i++) {
                packet = queue.poll();
                if (packet == null) {
                    this.check.set(0);// tell others to start doing send them self
                    done = true;
                    return true;
                }
                writeDataToSocket(packet);
            }
            done = true;
        } finally {
            if (!done) {
                this.check.set(2); // indicate this connection is bad
            }
        }
        this.exec.execute(new SpeedSteer());
        return true;
    }

    private class SpeedSteer implements Runnable {

        @Override
        public void run() {
            try {
                sendloop();
                check.set(0);   // tell others to start doing send
            } catch (IOException ex) {
                check.set(2);   // indicate this connection is bad
                Logger.getLogger(NetworkOut.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void sendloop() throws IOException {
            boolean flag = true;
            TransportPacket.Packet packet = null;
            int startCount = 0;
            int i = 0;
            do {
                if (startCount > 50) {
                    flag = false;
                }
                for (i = startCount; i < 100; i++) {
                    packet = queue.poll();
                    if (packet == null) {
                        startCount++;
                        continue;
                    }
                    writeDataToSocket(packet);
                    startCount--;
                }
                if (startCount < -20) {
                    startCount = 0;
                    flag = true;
                    continue;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                }
            } while (flag);
        }
    }

    private void writeDataToSocket(TransportPacket.Packet packet) throws IOException {
        try {
            ByteBuffer writeOutput = NetPacketDataConverter.writeOutput(packet);
            this.socket.getOutputStream().write(writeOutput.array());
//packet.writeDelimitedTo(this.socket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(NetworkOut.class.getName()).log(Level.SEVERE, null, ex);
            try {
                this.socket.close();
            } catch (IOException ex1) {
                Logger.getLogger(NetworkOut.class.getName()).log(Level.SEVERE, null, ex1);
            }
            this.openSocket();
        }
    }
}
