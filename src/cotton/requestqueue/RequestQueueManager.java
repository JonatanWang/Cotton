package cotton.requestqueue;

import cotton.network.NetworkPacket;
import cotton.network.Origin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import cotton.network.NetworkHandler;
import java.io.IOException;
import cotton.network.PathType;
import java.util.Set;
import java.util.ArrayList;
/**
 * @author Tony
 * @author Magnus
 */
public class RequestQueueManager{

    private ConcurrentHashMap<String,RequestQueue> internalQueueMap;
    private ExecutorService threadPool;    
    private NetworkHandler networkHandler;

    public RequestQueueManager(){
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
    }

    public RequestQueueManager(NetworkHandler networkHandler){
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        this.networkHandler = networkHandler;
    }

    public void setNetworkHandler(NetworkHandler networkHandler){
        this.networkHandler = networkHandler;
    }

    public String[] getActiveQueues(){
        Set<String> keys = internalQueueMap.keySet();
        ArrayList<String> names = new ArrayList<>();
        names.addAll(keys);
        String[] nameList = names.toArray(new String[names.size()]);
        return nameList;
    }

    private void startQueue(String serviceName){
        RequestQueue queuePool = new RequestQueue();
        internalQueueMap.putIfAbsent(serviceName,queuePool);
        
    }
    public void queueService(NetworkPacket packet,String serviceName){
        RequestQueue queue = internalQueueMap.get(serviceName);
        if(queue == null)
            return;
        queue.queueService(packet); 
        threadPool.execute(queue);
    }

    public void addAvailableInstance(Origin origin,String serviceName){
        RequestQueue queue = internalQueueMap.get(serviceName);
        if(queue == null)
            return;
        queue.addInstance(origin);
        threadPool.execute(queue);
    }

    public void stop(){
        threadPool.shutdown();
    }

    private class RequestQueue implements Runnable{
        private ConcurrentLinkedQueue<NetworkPacket> processQueue;
        private ConcurrentLinkedQueue<Origin> processingNodes;
        public RequestQueue(){
            processQueue = new ConcurrentLinkedQueue<>();
            processingNodes = new ConcurrentLinkedQueue<>();

        }

        public void queueService(NetworkPacket packet){
            processQueue.add(packet);
        } 

        public void addInstance(Origin origin){
            processingNodes.add(origin);
        }

        public void run(){
            Origin origin = null;
            while((origin = processingNodes.poll()) != null){
                NetworkPacket packet = processQueue.poll();
                if(packet == null){
                    processingNodes.add(origin);
                    return;
                }
                packet.setPathType(PathType.SERVICE);
                try{
                    networkHandler.send(packet,origin.getAddress());
                }catch(IOException e){
                    processQueue.add(packet);
                    // TODO: LOGGING
                }
            }
        }
    }
}
