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

import net.b2net.utils.iot.nio.io.AcceptSelectorHandler;
import net.b2net.utils.iot.nio.io.SelectorThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * Listens for incoming connections from clients, using a selector to receive
 * connect events. Therefore, instances of this class don't have an associated
 * thread. When a connection is established, it notifies a listener using a
 * callback.
 */
public final class Acceptor extends TagedHandler implements AcceptSelectorHandler
{

    private final static Logger logger = Logger.getLogger(Acceptor.class.getCanonicalName());

    // Used to receive incoming connections
    private ServerSocketChannel ssc;

    // The selector used by this instance.
    private final SelectorThread ioThread;

    // Port where to listen for connections.
    private final int listenPort;

    // Listener to be notified of new connections and of errors.
    private final AcceptorListener listener;

    // address to bind
    private final String address;

    // Nagel Algorithm desired state
    private final static boolean nagle = false;

    // KeepAlive desired state
    private final static boolean keepAlive = true;

    // how long to sleep if first accept didn't go
    private final static long acceptSleepTimeout = 5;

    // what is the maximum time to wiat to accept a socket connection
    private final static long maxWaitToAcceptSocket = 60 * 1000; // one min


    /**
     * Creates a new instance. No server socket is created. Use
     * openServerSocket() to start listening *
     *
     * @param listenPort The port to open.
     * @param listener   The object that will receive notifications of incoming
     *                   connections.
     * @param ioThread   The selector thread.
     */
    public Acceptor(
            int listenPort,
            SelectorThread ioThread,
            AcceptorListener listener)
    {
        this.ioThread = ioThread;
        this.listenPort = listenPort;
        this.listener = listener;
        this.address = null;
    }

    public Acceptor(
            String address,
            int listenPort,
            SelectorThread ioThread,
            AcceptorListener listener)
    {
        this.address = address;
        this.ioThread = ioThread;
        this.listenPort = listenPort;
        this.listener = listener;
    }

    /**
     * Starts listening for incoming connections. This method does not block
     * waiting for connections. Instead, it registers itself with the selector
     * to receive connect events.
     *
     * @throws IOException
     */
    public void openServerSocket() throws IOException
    {
        ssc = ServerSocketChannel.open();
        InetSocketAddress isa = null;
        if (address != null)
        {
            isa = new InetSocketAddress(
                    address,
                    listenPort);
        }
        else
        {
            isa = new InetSocketAddress(listenPort);
        }
        ssc.socket().bind(
                isa,
                100);

        // This method might be called from any messageForwarder. We must use
        // the xxxLater methods so that the actual register operation
        // is done by the selector's messageForwarder. No other messageForwarder should access
        // the selector directly.
        try
        {
            ioThread.registerChannel(
                    ssc,
                    SelectionKey.OP_ACCEPT,
                    this);
        }
        catch (Exception ex)
        {
            listener.socketError(
                    Acceptor.this,
                    ex);
        }
    }

    @Override
    public String toString()
    {
        return "ListenPort: " + listenPort;
    }


    /**
     * Called by SelectorThread when the underlying server socket is ready to
     * accept a connection. This method should not be called from anywhere else.
     */
    public void handleAccept()
    {
        SocketChannel sc = null;
        try
        {
            int countTry = 1;
            while ((sc = ssc.accept()) == null)
            {
                try
                {
                    countTry++;
                    Thread.sleep(acceptSleepTimeout);
                }
                catch (InterruptedException e)
                {
                }
                logger.info("Yield until able to finish connect...");
                if (maxWaitToAcceptSocket < (countTry * acceptSleepTimeout))
                {
                    logger.severe("Maximum wait to accept socket exceeded. - rejecting");
                    throw new IOException("Timeout for accept socket" + maxWaitToAcceptSocket + " msec exceeded");
                }
            }
            // Reactivate interest to receive the next connection. We
            // can use one of the XXXNow methods since this method is being
            // executed on the selector's messageForwarder.
            ioThread.addChannelInterest(
                    ssc,
                    SelectionKey.OP_ACCEPT);
        }
        catch (IOException e)
        {
            sc = null;
            listener.socketError(
                    this,
                    e);
        }

        if (sc != null)
        {
            // Connection established
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
                    // not intereseted
                }

                listener.socketConnected(
                        this,
                        sc);
            }

        }
    }

    /**
     * Closes the socket. Returns only when the socket has been closed.
     */
    public void close()
    {
        try
        {
            ssc.close();
        }
        catch (Exception e)
        {
            // Ignore
        }
    }
}
