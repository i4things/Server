package net.b2net.utils.iot.server;


import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.nio.handlers.Acceptor;
import net.b2net.utils.iot.nio.handlers.AcceptorListener;
import net.b2net.utils.iot.nio.handlers.PacketChannel;
import net.b2net.utils.iot.nio.handlers.PacketChannelListener;
import net.b2net.utils.iot.nio.io.SelectorThread;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
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
class JsonServer implements AcceptorListener, PacketChannelListener
{
    private static final Logger logger = Logger.getLogger(JsonServer.class.getCanonicalName());


    private final BlockingQueue<Runnable> workerQueueServerProcessing = new LinkedBlockingQueue<Runnable>();
    private int maxDrainSizeServerProcessing = 32;

    private SelectorThread st;
    private Acceptor acceptor;

    private final int port;
    private final String address;

    private final JsonProcessor processor;
    private final SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    // delay cleanup
    private final static int MAX_CONNECTIONS_PER_TIMEOUT = 512;
    private final static long MAX_TIME_WITHOUT_PACKET = 60 * 1000; // 1 min

    private final DoSPreventer preventer = new DoSPreventer(MAX_CONNECTIONS_PER_TIMEOUT, MAX_TIME_WITHOUT_PACKET);


    Thread workerThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                acceptor.openServerSocket();
            }
            catch (Exception ex)
            {
                Print.printStackTrace(ex, logger);
                System.exit(1);
            }

            logger.info("Json socket server started");

            Collection<Runnable> drainCollection = new ArrayList<Runnable>(maxDrainSizeServerProcessing);

            for (; ; )
            {
                drainCollection.clear();

                try
                {
                    int drainedElements = workerQueueServerProcessing.drainTo(drainCollection, maxDrainSizeServerProcessing);
                    if (drainedElements == 0)
                    {
                        drainCollection.add(workerQueueServerProcessing.take());
                        workerQueueServerProcessing.drainTo(drainCollection, maxDrainSizeServerProcessing - 1);
                    }

                    for (Runnable runnable : drainCollection)
                    {
                        try
                        {
                            runnable.run();
                        }
                        catch (Exception ex)
                        {
                            Print.printStackTrace(ex, logger);
                        }
                    }
                }
                catch (Exception e)
                {
                    Print.printStackTrace(e, logger);
                }
            }

        }
    }, "JsonServer.WorkThread");


    JsonServer(String address,
               int port,
               Store store)
    {
        this.address = address;
        this.port = port;
        this.processor = new JsonProcessor(store);
    }

    /**
     * Initializes the json server
     */
    void initialize()
    {
        try
        {
            logger.info("Initializing json socket server on port: " + this.port);

            this.st = new SelectorThread("JsonServer.SelectorThread");
            this.acceptor = new Acceptor(address, port, st, this);
        }
        catch (Exception ex)
        {
            Print.printStackTrace(ex, logger);
            System.exit(1);
        }
    }


    void start()
    {
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Handles connected client
     *
     * @param acceptor The acceptor that originated this event.
     * @param sc       The client`s socket channel
     */

    @Override
    public void socketConnected(Acceptor acceptor, SocketChannel sc)
    {
        PacketChannel pc = null;

        try
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.fine("Connection accepted: " + sc.getRemoteAddress());
            }


            pc = new PacketChannel(
                    sc,
                    true,
                    st,
                    new JsonProtocolDecoder(),
                    this);

            pc.reactivateReading();

            if (!preventer.newConnection(pc))
            {
                // not allowed - kill it
                pc.close();
            }
        }
        catch (Exception e)
        {
            Print.printStackTrace(e, logger);

            if (pc != null)
            {
                pc.close();
            }
        }
    }

    /**
     * Handles error in the client`s socket channel
     *
     * @param acceptor The acceptor where the error occurred.
     * @param ex
     */
    @Override
    public void socketError(Acceptor acceptor, Exception ex)
    {
        //To change body of implemented methods use File | Settings | File Templates.
        Print.printStackTrace(ex, logger);
    }

    /**
     * Handles received package from the client`s socket channel
     *
     * @param pc   The source of the event.
     * @param pckt The reassembled packet
     */
    @Override
    public void packetArrived(final PacketChannel pc, final ByteBuffer pckt)
    {
        try
        {
            workerQueueServerProcessing.put(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        byte[] r_b = new byte[pckt.remaining()];
                        pckt.get(r_b);
                        String r = new String(r_b, StandardCharsets.UTF_8).trim();

                        //                012
                        if (r.startsWith("GET"))
                        {
                            r = r.substring(3).trim();

                            byte[] rs = toHttpResponse(processor.process(r)).getBytes(StandardCharsets.UTF_8);

                            ByteBuffer res = ByteBuffer.allocate(rs.length).put(rs);
                            res.position(0);

                            pc.sendPacket(res);

                            preventer.newPacket(pc);

                        }
                    }
                    catch (Exception e)
                    {
                        Print.printStackTrace(e, logger);

                        if (pc != null)
                        {
                            pc.close();
                        }
                    }
                }
            });
        }
        catch (InterruptedException ex)
        {
            Print.printStackTrace(ex, logger);
        }
    }

    /**
     * Handles exception in the client`s socket channel
     *
     * @param pc The source of the event.
     * @param ex The exception representing the error.
     */
    @Override
    public void socketException(final PacketChannel pc, final Exception ex)
    {
        try
        {
            workerQueueServerProcessing.put(new Runnable()
            {
                @Override
                public void run()
                {
                    logger.fine("Socket exception for client " + pc.getRemoteAddress());
                    Print.printStackTrace(ex, logger);
                }
            });
        }
        catch (InterruptedException exx)
        {
            Print.printStackTrace(exx, logger);
        }
    }

    /**
     * Handles client`s socket disconnected
     *
     * @param pc     The source of the event.
     * @param failed Indication whether there have been an error in the connection
     */
    @Override
    public void socketDisconnected(final PacketChannel pc, final boolean failed)
    {
        try
        {
            workerQueueServerProcessing.put(new Runnable()
            {
                @Override
                public void run()
                {
                    logger.fine("Client goes away: " + pc.getRemoteAddress());
                }
            });
        }
        catch (InterruptedException ex)
        {
            Print.printStackTrace(ex, logger);
        }
    }


    private String toHttpResponse(String json)
    {
        Calendar calendar = Calendar.getInstance();

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n");
        sb.append("Date: ").append(httpDateFormat.format(calendar.getTime())).append("\r\n");
        sb.append("Server: B2N JasonSever");
        sb.append("Last-Modified: ").append(httpDateFormat.format(calendar.getTime())).append("\r\n");
        sb.append("Content-Length: ").append(json.getBytes(StandardCharsets.UTF_8).length).append("\r\n");
        sb.append("Content-Type: application/json\r\n");
        sb.append("Connection: Closed\r\n");
        sb.append("\r\n");
        sb.append(json);

        return sb.toString();
    }

}
