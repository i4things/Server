package net.b2net.utils.iot.server;

import net.b2net.utils.iot.nio.handlers.PacketChannel;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

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
class GatewayBuffer
{
    private Map<Long, DeleteList> data = new HashMap<Long, DeleteList>();
    private final Object sync = new Object();
    private final BlockingQueue<TimeoutElement> timeouted = new DelayQueue<TimeoutElement>();
    private final Map<Long, PacketChannel> gatewayCh = new HashMap<Long, PacketChannel>();
    private final Map<PacketChannel, Long> channelGw = new HashMap<PacketChannel, Long>();


    private class TimeoutElement implements Delayed
    {
        private final long gatewayId;
        private final DeleteListElement value;
        private final long trigger;

        TimeoutElement(long gatewayId,
                       DeleteListElement value)
        {
            this.gatewayId = gatewayId;
            this.value = value;
            this.trigger = System.nanoTime() + (Utils.NOTIFICATION_TIMEOUT * 1000000); // convert millis in nanos
        }

        final DeleteListElement getValue()
        {
            return value;
        }

        final long getGatewayId()
        {
            return gatewayId;
        }

        public final long getDelay(TimeUnit unit)
        {
            long n = trigger - System.nanoTime();
            return unit.convert(n, TimeUnit.NANOSECONDS);
        }

        public final int compareTo(Delayed o)
        {
            long oTrigger = ((TimeoutElement) o).trigger;
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
    }

    private final Thread cleanupThread = new Thread(new Runnable()
    {
        public void run()
        {
            for (; ; )
            {

                try
                {
                    TimeoutElement element = timeouted.take();
                    delete(element.getGatewayId(), element.getValue());
                }
                catch (InterruptedException ex)
                {
                    // not interested
                }

            }
        }
    }, "GatewayBuffer");

    GatewayBuffer()
    {
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    void addGatewayChannel(long gateway_id, PacketChannel pc)
    {
        synchronized (sync)
        {
            gatewayCh.put(new Long(gateway_id), pc);
            channelGw.put(pc, new Long(gateway_id));
        }
    }

    void removeGatewayChannel(PacketChannel pc)
    {
        synchronized (sync)
        {
            Long gateway_id = channelGw.remove(pc);
            if (gateway_id != null)
            {
                gatewayCh.remove(gateway_id);
            }
        }
    }

    void add(long gateway_id, ByteBuffer value)
    {
        DeleteListElement e = new DeleteListElement(value);

        synchronized (sync)
        {
            DeleteList l = data.get(new Long(gateway_id));
            if (l == null)
            {
                l = new DeleteList();
                data.put(new Long(gateway_id), l);
            }
            l.push(e);

            // if space is available and channel known push one message
            if ((l.getLastMessageCount() < Utils.MAX_MSG_HEARTBEAT) && (l.getLastBufferSize() < (Utils.MAX_BUFFER_SIZE_HEARTBEAT - Utils.MAX_NOTIFY_SIZE)))
            {
                PacketChannel pc = gatewayCh.get(new Long(gateway_id));
                if (pc != null)
                {
                    e = l.pop();

                    if (e != null)
                    {
                        l.incLastMessageCount();
                        l.incLastBufferSize(e.getValue().remaining());

                        pc.sendPacket(e.getValue());
                    }
                }
            }
        }

        try
        {
            timeouted.put(new TimeoutElement(gateway_id, e));
        }
        catch (InterruptedException ex)
        {
            // not interested
        }
    }

    void get(ByteBuffer buffer, long gateway_id)
    {
        DeleteList l = null;
        synchronized (sync)
        {
            l = data.get(new Long(gateway_id));
        }

        if (l == null)
        {
            return;
        }

        l.clearLast();

        for (int i = 0; (i < Utils.MAX_MSG_HEARTBEAT) && (buffer.position() < (Utils.MAX_BUFFER_SIZE_HEARTBEAT - Utils.MAX_NOTIFY_SIZE)); i++)
        {
            DeleteListElement e = null;

            synchronized (sync)
            {
                e = l.pop();

                if (e == null)
                {
                    break;
                }

                l.incLastMessageCount();
                l.incLastBufferSize(e.getValue().remaining());
            }

            buffer.put(e.getValue());
        }

    }

    void delete(long gateway_id, DeleteListElement e)
    {
        synchronized (sync)
        {
            DeleteList l = data.get(new Long(gateway_id));
            if (l == null)
            {
                return;
            }

            l.delete(e);
        }
    }


}
