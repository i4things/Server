package  net.b2net.utils.iot.server;

import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.nio.handlers.PacketChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS
 * OF THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 * <p/>
 * IN PARTICULAR, YOU WILL INDEMNIFY AND HOLD B2N LTD., ITS
 * RELATED COMPANIES AND ITS SUPPLIERS, HARMLESS FROM AND AGAINST ANY
 * CLAIMS OR LIABILITIES ARISING OUT OF THE USE, REPRODUCTION, OR
 * DISTRIBUTION OF YOUR PROGRAMS, INCLUDING ANY CLAIMS OR LIABILITIES
 * ARISING OUT OF OR RESULTING FROM THE USE, MODIFICATION, OR
 * DISTRIBUTION OF PROGRAMS OR FILES CREATED FROM, BASED ON, AND/OR
 * DERIVED FROM THIS SOURCE CODE FILE.
 */
class DoSPreventer
{
    private static final Logger logger = Logger.getLogger(DoSPreventer.class.getCanonicalName());

    private final int MAX_CONNECTIONS_PER_TIMEOUT;
    private final long MAX_TIME_WITHOUT_PACKET;

    private interface Executable
    {
        // if true reinsert
        public boolean execute();
    }

    private static class DelayClean implements Delayed
    {
        private long trigger;
        private final Executable task;

        DelayClean(long delayMSec,
                   Executable task)
        {
            this.trigger = System.nanoTime() + (delayMSec * 1000000); // convert millis in nanos
            this.task = task;
        }

        void setDelay(long delayMSec)
        {
            this.trigger = System.nanoTime() + (delayMSec * 1000000); // convert millis in nanos
        }

        @Override
        public long getDelay(TimeUnit unit)
        {
            long n = trigger - System.nanoTime();
            return unit.convert(n, TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o)
        {
            long oTrigger = ((DelayClean) o).trigger;
            if (trigger < oTrigger)
            {
                return -1;
            }
            else if (trigger > oTrigger)
            {
                return 1;
            }
            return 0;
        }

        Executable getTask()
        {
            return task;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            {
                return false;
            }
            if (!(obj instanceof DelayClean))
            {
                return false;
            }
            return ((DelayClean) obj).getTask().equals(task);
        }
    }

    private final long CONNECTION_RESET_TIMEOUT = 5000; // 5 sec
    private final BlockingQueue<DelayClean> workerQueue = new DelayQueue<DelayClean>();
    private final Thread cleanupThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            for (; ; )
            {
                try
                {
                    DelayClean task = workerQueue.take();

                    if (task.getTask().execute())
                    {
                        task.setDelay(MAX_TIME_WITHOUT_PACKET);
                        workerQueue.put(task);
                    }
                }
                catch (Exception ex)
                {
                    Print.printStackTrace(ex, logger);
                }
            }
        }
    }, "DoSPreventer:worker");


    private final Object sync = new Object();
    private final Map<String, AtomicInteger> ipRefCount = new HashMap<String, AtomicInteger>();
    private final Map<PacketChannel, AtomicLong> activeConn = new HashMap<PacketChannel, AtomicLong>();

    DoSPreventer(int MAX_CONNECTIONS_PER_5_SEC,
                 long MAX_TIME_WITHOUT_PACKET)
    {
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        this.MAX_CONNECTIONS_PER_TIMEOUT = MAX_CONNECTIONS_PER_5_SEC;
        this.MAX_TIME_WITHOUT_PACKET = MAX_TIME_WITHOUT_PACKET;
    }

    // return true if allowed
    boolean newConnection(final PacketChannel pc)
    {
        synchronized (sync)
        {
            AtomicInteger refCount = ipRefCount.get(pc.getRemoteIPAddress());
            boolean ret = true;
            if (refCount == null)
            {
                try
                {
                    ipRefCount.put(pc.getRemoteIPAddress(), new AtomicInteger(1));
                    workerQueue.put(new DelayClean(CONNECTION_RESET_TIMEOUT, new Executable()
                    {
                        @Override
                        public boolean execute()
                        {
                            synchronized (sync)
                            {
                                ipRefCount.remove(pc.getRemoteIPAddress());
                            }

                            return false;
                        }
                    }));
                }
                catch (Exception ex)
                {
                    Print.printStackTrace(ex, logger);
                    synchronized (sync)
                    {
                        ipRefCount.clear();
                    }
                }
            }
            else
            {
                ret = (refCount.incrementAndGet() <= MAX_CONNECTIONS_PER_TIMEOUT);
            }

            if (ret)
            {
                try
                {
                    activeConn.put(pc, new AtomicLong(System.currentTimeMillis()));
                    workerQueue.put(new DelayClean(MAX_TIME_WITHOUT_PACKET, new Executable()
                    {
                        @Override
                        public boolean execute()
                        {
                            synchronized (sync)
                            {
                                AtomicLong last = activeConn.get(pc);
                                if (last != null)
                                {
                                    if ((System.currentTimeMillis() - last.get()) < MAX_TIME_WITHOUT_PACKET)
                                    {
                                        // all OK still active
                                        return true;
                                    }

                                    // no activity for required time
                                    activeConn.remove(pc);
                                }
                            }

                            // not found or no activity - make sure we are closed
                            pc.close();
                            return false;
                        }
                    }));
                }
                catch (Exception ex)
                {
                    Print.printStackTrace(ex, logger);
                    synchronized (sync)
                    {
                        activeConn.clear();
                    }
                }
            }
            return ret;

        }
    }

    void newPacket(PacketChannel pc)
    {
        synchronized (sync)
        {
            AtomicLong last = activeConn.get(pc);
            if (last != null)
            {
                last.set(System.currentTimeMillis());
            }
        }
    }
}
