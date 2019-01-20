package net.b2net.utils.iot.server;


import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.nio.handlers.Acceptor;
import net.b2net.utils.iot.nio.handlers.AcceptorListener;
import net.b2net.utils.iot.nio.handlers.PacketChannel;
import net.b2net.utils.iot.nio.handlers.PacketChannelListener;
import net.b2net.utils.iot.nio.io.SelectorThread;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
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
class Server implements AcceptorListener, PacketChannelListener
{
    private static final Logger logger = Logger.getLogger(Server.class.getCanonicalName());

    private final BlockingQueue<Runnable> workerQueueServerProcessing = new LinkedBlockingQueue<Runnable>();
    private int maxDrainSizeServerProcessing = 32;


    private SelectorThread st;
    private Acceptor acceptor;

    private int port;
    private final String address;
    private final Processor processor;

    private final static int MAX_CONNECTIONS_PER_TIMEOUT = 5;
    private final static long MAX_TIME_WITHOUT_PACKET = 20 * 60 * 1000; // 20 min

    private final DoSPreventer preventer = new DoSPreventer(MAX_CONNECTIONS_PER_TIMEOUT, MAX_TIME_WITHOUT_PACKET);

    Server(String address,
           int port,
           Store store)
    {
        this.address = address;
        this.port = port;
        this.processor = new Processor(store);
    }

    /**
     * Initializes the iot server
     */
    void initialize()
    {
        try
        {
            logger.info("Initializing IoT socket server on port: " + this.port);

            this.st = new SelectorThread("IoTServer.SelectorThread");
            this.acceptor = new Acceptor(address, port, st, this);
        }
        catch (Exception ex)
        {
            Print.printStackTrace(ex, logger);
            System.exit(1);
        }
    }


    /**
     * Starts the web socket server
     */
    void run()
    {
        try
        {
            this.acceptor.openServerSocket();
        }
        catch (Exception ex)
        {
            Print.printStackTrace(ex, logger);
            System.exit(1);
        }

        logger.info("IoT socket server started");

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
                    new ProtocolDecoder(),
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
                        if (!processor.process(pckt, pc))
                        {
                            // in case protocol mismatch - close connection
                            pc.close();
                        }
                        else
                        {
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
                    processor.disconnect(pc);
                    logger.fine("Client goes away: " + pc.getRemoteAddress());
                }
            });
        }
        catch (InterruptedException ex)
        {
            Print.printStackTrace(ex, logger);
        }
    }


}
