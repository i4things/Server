/**
 * Copyright (c) 2003-2011 B2N Ltd. All Rights Reserved.
 *
 * This SOURCE CODE FILE, which has been provided by B2N Ltd. as part
 * of an B2N Ltd. product for use ONLY by licensed users of the product,
 * includes CONFIDENTIAL and PROPRIETARY information of B2N Ltd.
 *
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS
 * OF THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 *
 * IN PARTICULAR, YOU WILL INDEMNIFY AND HOLD B2N LTD., ITS
 * RELATED COMPANIES AND ITS SUPPLIERS, HARMLESS FROM AND AGAINST ANY
 * CLAIMS OR LIABILITIES ARISING OUT OF THE USE, REPRODUCTION, OR
 * DISTRIBUTION OF YOUR PROGRAMS, INCLUDING ANY CLAIMS OR LIABILITIES
 * ARISING OUT OF OR RESULTING FROM THE USE, MODIFICATION, OR
 * DISTRIBUTION OF PROGRAMS OR FILES CREATED FROM, BASED ON, AND/OR
 * DERIVED FROM THIS SOURCE CODE FILE.
 *
 * @author V.Tomanov
 * @version 1.2
 */

package net.b2net.utils.iot.nio.handlers;

import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.nio.io.ConnectorSelectorHandler;
import net.b2net.utils.iot.nio.io.SelectorThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a non-blocking connection attempt to a remote host.
 */
public final class Connector extends TagedHandler implements ConnectorSelectorHandler
{
    private final static Logger logger = Logger.getLogger(Connector.class.getCanonicalName());

    // The socket being connected.
    private SocketChannel sc;

    // The address of the remote endpoint.
    private final InetSocketAddress remoteAddress;

    // The selector used for receiving events.
    private final SelectorThread selectorThread;

    // The listener for the callback events.
    private final ConnectorListener listener;

    // Nagel Algorithm desired state
    private final static boolean nagle = false;

    // KeepAlive desired state
    private final static boolean keepAlive = true;

    // how long to sleep before try again if finish connect is not done
    private final static long finishConnectTimeout = 5;

    /**
     * Creates a new instance. The connection is not attempted here. Use
     * connect() to start the attempt.
     *
     * @param remoteAddress The remote endpoint where to connect.
     * @param listener      The object that will receive the callbacks from this
     *                      Connector.
     * @param selector      The selector to be used.
     * @throws IOException
     */
    public Connector(
            SelectorThread selector,
            InetSocketAddress remoteAddress,
            ConnectorListener listener)
    {
        this.selectorThread = selector;
        this.remoteAddress = remoteAddress;
        this.listener = listener;
    }

    /**
     * Starts a non-blocking connection attempt.
     *
     * @throws IOException
     */
    public void connect() throws IOException
    {
        try
        {
            sc = SocketChannel.open();
            // Very important. Set to non-blocking. Otherwise a call
            // to connect will block until the connection attempt fails
            // or succeeds.
            sc.configureBlocking(false);

            if (!sc.connect(remoteAddress))
            {

                // Registers itself to receive the connect event.
                try
                {
                    selectorThread.registerChannel(
                            sc,
                            SelectionKey.OP_CONNECT,
                            this);
                }
                catch (Exception ex)
                {
                    listener.connectionFailed(
                            Connector.this,
                            ex);
                }
                return;
            }
        }
        catch (final Exception ex)
        {
            listener.connectionFailed(
                    Connector.this,
                    ex);

            try
            {
                // selectorThread.removeChannelInterestNow(sc,
                // SelectionKey.OP_CONNECT);
                if (sc != null)
                {
                    sc.close();
                }
            }
            catch (Exception e)
            {
                Print.printStackTrace(
                        e,
                        logger);
            }

            return;
        }

        while (!sc.finishConnect())
        {
            try
            {
                Thread.sleep(finishConnectTimeout);
            }
            catch (InterruptedException e)
            {
            }
            if (logger.isLoggable(Level.FINE))
            {
                logger.fine("Yield until able to finish connect...");
            }
        }

        final Connector me = this;

        // we need to execute this on the selector thread

        selectorThread.invoke(
                new Runnable()
                {
                    public void run()
                    {
                        // we have imediate connection
                        Socket socket = sc.socket();

                        if (socket != null)
                        {
                            try
                            {
                                socket.setTcpNoDelay(!nagle);
                                socket.setKeepAlive(keepAlive);
                            }
                            catch (Exception e)
                            {
                                // not interested
                            }
                        }
                        else
                        {
                            try
                            {
                                throw new Exception("Socket == null after connect!");
                            }
                            catch (Exception ex)
                            {
                                Print.printStackTrace(ex, logger);
                            }
                        }

                        listener.connectionEstablished(
                                me,
                                sc);
                    }
                }
        );


    }


    /**
     * Called by the selector thread when the connection is ready to be
     * completed.
     */
    public boolean handleConnect()
    {
        try
        {
            while (!sc.finishConnect())
            {
                try
                {
                    Thread.sleep(finishConnectTimeout);
                }
                catch (InterruptedException e)
                {
                }
                if (logger.isLoggable(Level.FINE))
                {
                    logger.fine("Yield until able to finish connect...");
                }
            }

            Socket socket = sc.socket();

            if (socket != null)
            {
                try
                {
                    socket.setTcpNoDelay(!nagle);
                    socket.setKeepAlive(keepAlive);
                }
                catch (Exception e)
                {
                    // not interested
                }
            }
            else
            {
                listener.connectionFailed(
                        this,
                        new Exception("Unable to connect"));
                // sc is null
                return false;
            }

            // Connection succeeded
            listener.connectionEstablished(
                    this,
                    sc);
            return true;
        }
        catch (IOException ex)
        {
            // Could not connect.
            listener.connectionFailed(
                    this,
                    ex);
        }

        try
        {
            // selectorThread.removeChannelInterestNow(sc,
            // SelectionKey.OP_CONNECT);
            if (sc != null)
            {
                sc.close();
            }
        }
        catch (IOException e)
        {
            Print.printStackTrace(
                    e,
                    logger);
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "Remote endpoint: " + remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();
    }
}
