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

package net.b2net.utils.iot.nio.io;

import net.b2net.utils.iot.common.logger.Print;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Event queue for I/O events raised by a selector. This class receives the
 * lower level events raised by a Selector and dispatches them to the
 * appropriate handler. It also manages all other operations on the selector,
 * like registering and unregistered channels, or updating the events of
 * interest for each monitored socket. This class is inspired on the
 * java.awt.EventQueue and follows a similar model. The EventQueue class is
 * responsible for making sure that all operations on AWT objects are performed
 * on a single thread, the one managed internally by EventQueue. The
 * SelectorThread class performs a similar task. In particular: - Only the
 * thread created by instances of this class should be allowed to access the
 * selector and all sockets managed by it. This means that all I/O operations on
 * the sockets should be peformed on the corresponding selector's thread. If
 * some other thread wants to access objects managed by this selector, then it
 * should use <code>invokeLater()</code> or the <code>invokeAndWait()</code>
 * to dispatch a runnable to this thread. - This thread should not be used to
 * perform lenghty operations. In particular, it should never be used to perform
 * blocking I/O operations. To perform a time consuming task use a worker
 * thread. This architecture is required for two main reasons: The first, is to
 * make synchronization in the objects of a connection unnecessary. This is good
 * for performance and essential for keeping the complexity low. Getting
 * synchronization right within the objects of a connection would be extremely
 * tricky. The second is to make sure that all actions over the selector, its
 * keys and related sockets are carried in the same thread. My personal
 * experience with selectors is that they don't work well when being accessed
 * concurrently by several threads. This is mostly the result of bugs in some of
 * the version of Sun's Java SDK (these problems were found with version
 * 1.4.2_02). Some of the bugs have already been identified and fixed by Sun.
 * But it is better to work around them by avoiding multithreaded access to the
 * selector.
 */
public final class SelectorThread implements Runnable
{

    /*
     * Logger
     */
    private static final Logger logger = Logger.getLogger(SelectorThread.class.getCanonicalName());
    /**
     * Selector used for I/O multiplexing
     */
    private final Selector selector;

    /**
     * The thread associated with this selector
     */
    private final Thread selectorThread;

    /**
     * Flag telling if this object should terminate, that is, if it should close
     * the selector and kill the associated thread. Used for graceful
     * termination.
     */
    private volatile boolean closeRequested = false;

    private final List<Runnable> pendingInvocationsBuffer = new ArrayList<Runnable>(16);
    private final BlockingQueue<Runnable> pendingInvocations = new LinkedBlockingQueue<Runnable>();
    public volatile boolean pumpCacheNeeded = false;
    private final int cachePumpTimeout = 1000;
    private volatile boolean pumpStop = false;
    private final Thread pumpCacheThread;
    private final Runnable pumpCacheTask = new Runnable()
    {
        public void run()
        {
            while (!pumpStop)
            {
                pumpCacheNeeded = true;
                selector.wakeup();
                try
                {
                    Thread.sleep(cachePumpTimeout);
                }
                catch (InterruptedException e)
                {
                    // not interested
                }
            }

            // make sure we wait enough for the Selector thread to die
            for (int i = 0; i < 10; i++)
            {
                if (selectorThread.getState() != Thread.State.TERMINATED)
                {

                    selector.wakeup();

                    try
                    {
                        selectorThread.join(1000);
                    }
                    catch (InterruptedException e)
                    {
                        // not interested
                    }
                }
            }

            if (selectorThread.getState() != Thread.State.TERMINATED)
            {
                logger.severe("Selector thread do not want to die : " + selectorThread.getName());
            }
            else
            {
                logger.info("Selector thread : " + selectorThread.getName() + " is shutdown.");
            }

        }
    };

    /**
     * Creates a new selector and the associated thread. The thread is started
     * by this constructor, thereby making this object ready to be used.
     */

    public int pendingInvocationsSize()
    {
        return pendingInvocations.size();
    }

    public SelectorThread(
            String name) throws IOException
    {
        selector = Selector.open();
        selectorThread = new Thread(
                this,
                name);
        selectorThread.setDaemon(true);
        selectorThread.start();

        pumpCacheThread = new Thread(pumpCacheTask, name + ".PumpCache");
        pumpCacheThread.setDaemon(true);
        pumpCacheThread.start();
    }

    public final void requestClose()
    {

        closeRequested = true;
        // Nudges the selector.
        selector.wakeup();

        pumpStop = true;
    }

    public final void addChannelInterest(
            final SelectableChannel channel,
            final int interest) throws IOException
    {


        if (!isSelectorThread())
        {
            try
            {
                throw new Exception("addChannelInterest called on non selector thread");
            }
            catch (Exception ex)
            {
                Print.printStackTrace(ex, logger);
            }
        }
        else
        {
            SelectionKey sk = channel.keyFor(selector);

            if ((sk == null) || (!sk.isValid()))
            {
                // connection already closed
                return;

            }
            changeKeyInterest(
                    sk,
                    sk.interestOps() | interest);
        }
    }

    /**
     * Removes an interest from the list of events where a channel is
     * registered. The associated event handler will stop receiving events for
     * the specified interest. <p/> This method should only be called on the
     * selector thread. Otherwise an exception is thrown. Use the
     * removeChannelInterestLater() when calling from another thread.
     */
    public final void removeChannelInterest(
            final SelectableChannel channel,
            final int interest) throws IOException
    {

        if (!isSelectorThread())
        {
            try
            {
                throw new Exception("removeChannelInterest called on non selector thread");
            }
            catch (Exception ex)
            {
                Print.printStackTrace(ex, logger);
            }
        }
        else
        {
            SelectionKey sk = channel.keyFor(selector);

            if ((sk == null) || (!sk.isValid()))
            {
                // connection already closed
                return;

            }

            changeKeyInterest(
                    sk,
                    sk.interestOps() & ~interest);
        }
    }

    /**
     * Updates the interest set associated with a selection key. The old
     * interest is discarded, being replaced by the new one.
     */
    private void changeKeyInterest(
            final SelectionKey sk,
            final int newInterest) throws IOException
    {
        /*
         * This method might throw two unchecked exceptions: 1.
         * IllegalArgumentException - Should never happen. It is a bug if it
         * happens 2. CancelledKeyException - Might happen if the channel is
         * closed while a packet is being dispatched.
         */
        try
        {
            sk.interestOps(newInterest);
        }
        catch (CancelledKeyException cke)
        {
            IOException ioe = new IOException(
                    "Failed to change channel interest.");
            ioe.initCause(cke);
            throw ioe;
        }
    }

    /**
     * Registers a SelectableChannel with this selector. This channel will start
     * to be monitored by the selector for the set of events associated with it.
     * When an event is raised, the corresponding handler is called. <p/> This
     * method can be called multiple times with the same channel and selector.
     * Subsequent calls update the associated interest set and selector handler
     * to the ones given as arguments. <p/> This method should only be called on
     * the selector thread. Otherwise an exception is thrown. Use the
     * registerChannelLater() when calling from another thread.
     */
    public final void registerChannel(
            final SelectableChannel channel,
            final int selectionKeys,
            final SelectorHandler handlerInfo) throws IOException
    {
        if (!channel.isOpen())
        {
            throw new IOException("Channel is not open.");
        }

        try
        {
            if (channel.isRegistered())
            {
                SelectionKey sk = channel.keyFor(selector);
                sk.interestOps(selectionKeys);
                sk.attach(handlerInfo);
            }
            else
            {
                channel.configureBlocking(false);

                if (!isSelectorThread())
                {
                    invoke(
                            new Runnable()
                            {
                                public void run()
                                {
                                    try
                                    {
                                        channel.register(
                                                selector,
                                                selectionKeys,
                                                handlerInfo);
                                    }
                                    catch (ClosedChannelException e)
                                    {
                                    }
                                }
                            });
                }
                else
                {
                    channel.register(
                            selector,
                            selectionKeys,
                            handlerInfo);
                }
            }
        }
        catch (Exception e)
        {
            IOException ioe = new IOException("Error registering channel.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    public final void invoke(Runnable runnable)
    {
        pendingInvocations.add(runnable);
    }

    public final void wakeup()
    {
        selector.wakeup();
    }

    private void performInvocations()
    {

        pendingInvocations.drainTo(pendingInvocationsBuffer);

        for (Runnable r : pendingInvocationsBuffer)
        {
            r.run();
        }

        pendingInvocationsBuffer.clear();

    }

    private void performPumpCache()
    {
        pumpCacheNeeded = false;

        for (SelectionKey sk : selector.keys())
        {
            SelectorHandler handler = (SelectorHandler) sk.attachment();
            if ((handler == null) || (!sk.isValid()))
            {
                continue;
            }

            if (handler instanceof ReadWriteSelectorHandler)
            {
                ((ReadWriteSelectorHandler) handler).pumpCache();
            }
        }
    }

    /**
     * Main cycle. This is where event processing and dispatching happens.
     */
    public void run()
    {

        while (!closeRequested)
        {
            try
            {
                performInvocations();

                int selectedKeys;
                try
                {
                    if (pendingInvocations.size() > 0)
                    {
                        selectedKeys = selector.selectNow();
                    }
                    else
                    {
                        /// no need for timeout here as we have wakeup every 1 sec from the cache pump
                        selectedKeys = selector.select();
                    }

                    if (pumpCacheNeeded)
                    {
                        performPumpCache();
                    }
                }
                catch (Exception e)
                {
                    // Select should never throw an exception under normal
                    // operation. If this happens, print the error and try to
                    // continue working.
                    Print.printStackTrace(e, logger);
                    continue;
                }


                // Time to terminate?
                if (closeRequested)
                {
                    break;
                }

                if (selectedKeys == 0)
                {
                    // Go back to the beginning of the loop
                    continue;
                }

                // Someone is ready for IO, get the ready keys
                Set<SelectionKey> keys = selector.selectedKeys();
                // Walk through the collection of ready keys and dispatch
                // any active event.

                for (final SelectionKey sk : keys)
                {
                    try
                    {
                        // Some of the operations set in the selection key
                        // might no longer be valid when the handler is executed.
                        // So handlers should take precautions against this
                        // possibility.

                        final SelectorHandler handler = (SelectorHandler) sk.attachment();
                        if ((handler == null) || (!sk.isValid()))
                        {
                            continue;
                        }

                        // Obtain the interest of the key
                        final int readyOps = sk.readyOps();

                        int storeReadOp = readyOps & SelectionKey.OP_READ;
                        int storeWriteOp = readyOps & SelectionKey.OP_WRITE;
                        //clear the ops that will be processed except read - we need read to stay and write will be cleared
                        // in the handler
                        sk.interestOps((sk.interestOps() & ~readyOps) | storeReadOp | storeWriteOp);

                        performOps(
                                readyOps,
                                handler,
                                sk);
                    }
                    catch (Exception ex)
                    {
                        Print.printStackTrace(ex, logger);
                        // ignore
                    }
                }
                keys.clear();
            }
            catch (Exception e)
            {
                Print.printStackTrace(e, logger);
            }
        }

        closeSelectorAndChannels();
    }

    private void performOps(
            int readyOps,
            SelectorHandler handler,
            SelectionKey sk)
    {

        // Check what are the interests that are active and
        // dispatch the event to the appropriate method.

        // the key can be mixed accept/read for example!
        if ((readyOps & SelectionKey.OP_ACCEPT) != 0)
        {
            // A connection is ready to be completed
            ((AcceptSelectorHandler) handler).handleAccept();
        }

        if ((readyOps & SelectionKey.OP_CONNECT) != 0)
        {
            // A connection is ready to be accepted
            ((ConnectorSelectorHandler) handler).handleConnect();
        }

        // Readable or writable
        if ((readyOps & SelectionKey.OP_READ) != 0)
        {
            // It is possible to read
            ((ReadWriteSelectorHandler) handler).handleRead();
        }

        // Check if the key is still valid, since it might
        // have been invalidated in the read handler
        // (for instance, the socket might have been closed)
        if (sk.isValid() && ((readyOps & SelectionKey.OP_WRITE) != 0))
        {
            // It is ready to write
            ((ReadWriteSelectorHandler) handler).handleWrite(sk);
        }
    }

    /**
     * Closes all channels registered with the selector. Used to clean up when
     * the selector dies and cannot be recovered.
     */
    private void closeSelectorAndChannels()
    {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys)
        {
            try
            {
                key.channel().close();
            }
            catch (IOException e)
            {
                // Ignore
            }
        }
        try
        {
            requestClose();
            selector.close();
        }
        catch (IOException e)
        {
            // Ignore
        }
    }

    /**
     * Return true if the current thread is the selector thread
     *
     * @return boolean
     */
    public final boolean isSelectorThread()
    {
        return Thread.currentThread() == selectorThread;
    }

}
