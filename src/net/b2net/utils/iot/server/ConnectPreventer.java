package  net.b2net.utils.iot.server;


import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.nio.handlers.PacketChannel;
import sun.security.x509.IPAddressName;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
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
class ConnectPreventer
{
    private static final Logger logger = Logger.getLogger(ConnectPreventer.class.getCanonicalName());

    private final long BACK_OFF_TIMEOUT;

    private interface Executable
    {
        public void execute();
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

                    task.getTask().execute();
                }
                catch (Exception ex)
                {
                    Print.printStackTrace(ex, logger);
                }
            }
        }
    }, "ConnectPreventer:worker");


    private final Object sync = new Object();
    private final Set<String> ip = new HashSet<String>();

    ConnectPreventer(long BACK_OFF_TIMEOUT)
    {
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        this.BACK_OFF_TIMEOUT = BACK_OFF_TIMEOUT;
    }

    // return true if allowed
    boolean checkConnection(final PacketChannel pc)
    {
        synchronized (sync)
        {
            if (ip.contains(pc.getRemoteIPAddress()))
            {
                return false;
            }

            return true;
        }
    }

    void addConnection(PacketChannel pc)
    {
        synchronized (sync)
        {
            final String ipa = pc.getRemoteIPAddress();
            logger.info("ADD TO PREVENT CONNECT SET[" + ipa + "]");
            ip.add(ipa);
            try
            {
                workerQueue.put(new DelayClean(BACK_OFF_TIMEOUT, new Executable()
                {
                    @Override
                    public void execute()
                    {
                        synchronized (sync)
                        {
                            logger.info("REMOVE FROM PREVENT CONNECT SET[" + ipa + "]");
                            ip.remove(ipa);
                        }

                    }
                }));
            }
            catch (InterruptedException ex)
            {
                Print.printStackTrace(ex, logger);
                synchronized (sync)
                {
                    ip.clear();
                }
            }
        }
    }
}

