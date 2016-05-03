package cotton.requestqueue;

import cotton.network.NetworkPacket;
import cotton.network.Origin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * @author Tony
 * @author Magnus
 */
public class RequestQueueManager implements Runnable{

    private ConcurrentHashMap<String,RequestQueue> internalQueueMap;
    private ExecutorService threadPool;

    public RequestQueueManager(){
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();

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
    }

    public void addAvailableInstance(Origin origin,String serviceName){
        
    }

    public void run(){
        
    }

    private class RequestQueue{
        private ConcurrentLinkedQueue<NetworkPacket> processQueue;
        public RequestQueue(){
            processQueue = new ConcurrentLinkedQueue<>();
        }

        public void queueService(NetworkPacket packet){
            processQueue.add(packet);
        }
    }
}
