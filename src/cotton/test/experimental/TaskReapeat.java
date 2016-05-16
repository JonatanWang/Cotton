/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.test.experimental;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author o_0
 */
public class TaskReapeat {

    private final ScheduledExecutorService taskScheduler;

    public TaskReapeat() {
        this.taskScheduler = Executors.newScheduledThreadPool(2);
    }
    
    public void shutdown( ){
        this.taskScheduler.shutdown();
    }

    public static void main(String[] args) {
        TaskReapeat taskReapeat = new TaskReapeat();
        taskReapeat.tryAnnounceLater();
        try {
            Thread.sleep(50000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        taskReapeat.shutdown();
    }

    public boolean announce() {
        System.out.println("Hej");
        return new Random().nextBoolean();
    }
    private long announceDelay = 1;
    private ScheduledFuture<?> announceLaterTask = null;

    private void tryAnnounceLater() {
        announceDelay = (announceDelay < 10) ? announceDelay : 10;
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                if (announce()) {
                    System.out.println("Announce: done");
                } else {
                    tryAnnounceLater();
                    System.out.println("Announce: failed");
                }
                announceDelay++;

            }
        };

        System.out.println("try Announce in: " + announceDelay + " seconds again");

        final ScheduledFuture<?> schedule = this.taskScheduler.schedule(run, announceDelay, TimeUnit.SECONDS);
        this.taskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                schedule.cancel(true);
            }
        }, announceDelay + 10, TimeUnit.SECONDS);
    }
}
