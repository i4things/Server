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
import net.b2net.utils.iot.nio.io.ProtocolDecoder;
import net.b2net.utils.iot.nio.io.ReadWriteSelectorHandler;
import net.b2net.utils.iot.nio.io.SelectorThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Uses non-blocking operations to read and write from a socket. Internally,                                    s
 * this class uses a selector to receive read and write events from the
 * underlying socket.
 * <p/>
 * Methods on this class should be called only by the selector's thread
 * (including the constructor). If necessary, use Selector.invokeLater() to
 * dispatch a invocation to the selector's thread.
 */
public final class PacketChannel extends TagedHandler implements ReadWriteSelectorHandler
{

    private final static Logger logger = Logger.getLogger(PacketChannel.class.getCanonicalName());

    private final static int socketBufferSize = 32768;

    /**
     * The associated selector.
     */
    protected final SelectorThread selector;

    /**
     * The socket where read and write operations are performed.
     */
    private final SocketChannel sc;

    /**
     * Remote address
     */
    private final String remoteAddress;

    /**
     * Remote address
     */
    private final String remoteIPAddress;

    // outgoing buffers and queue
    private final static int cacheDenominator = 4;
    private ByteBuffer cacheBuffer = ByteBuffer.allocate(socketBufferSize / cacheDenominator);
    private final Object cacheBufferMutex = new Object();
    private ByteBuffer outBuffer = null;
    private ByteBuffer directBuffer = ByteBuffer.allocateDirect(socketBufferSize);
    private final BlockingQueue<ByteBuffer> outQueue = new LinkedBlockingQueue<ByteBuffer>();
    private final static int maxDrain = cacheDenominator;
    private final BlockingQueue<ByteBuffer> cachePool = new ArrayBlockingQueue<ByteBuffer>(1024);
    /**
     * Used to convert raw bytes into packets. (Strategy design pattern)
     */
    private final ProtocolDecoder protocolDecoder;

    /**
     * Object interested in the events generated by this class. It is notified
     * whenever an error occurs or a packet is read.
     */
    private final PacketChannelListener listener;

    /**
     * Indicate if this channel is already closed
     */
    private boolean closed = false;
    private final Object closedMutex = new Object();

    /**
     * Decoding thread
     */
    //private final Thread decodeThread;

    private final ByteBuffer inBuffer = ByteBuffer.allocate((socketBufferSize * 2) + 8);
    private volatile boolean performed = false;
    private final Object reactivateWritingMutex = new Object();
    private boolean isWritingScheduled = false;
    private final Runnable reactivateWriting = new Runnable()
    {
        public void run()
        {
            try
            {
                selector.addChannelInterest(sc, SelectionKey.OP_WRITE);
            }
            catch (Exception e)
            {
                Print.printStackTrace(e, logger);
            }
        }
    };

    // do we want to close connection if we get too big queue

    private static int limitOutQueueSize = 100000; // 0 means no limit

    static
    {
        //set in authority configurate xml or using commad line argument -Dmh.limit.out.queue.size="50000"
        String limitOutQueueSizeProperty = System.getProperty("mh.limit.out.queue.size");
        if (limitOutQueueSizeProperty != null)
        {
            try
            {
                limitOutQueueSize = Integer.parseInt(limitOutQueueSizeProperty);
                logger.info("Packet channel queue limit: " + limitOutQueueSize);
            }
            catch (NumberFormatException e)
            {
                Print.printStackTrace(e, logger);
            }
        }
    }

    // is a server channel
    private final boolean isServerChannel;

    /**
     * Creates and initializes a new instance. Read interest is enabled by the
     * constructor, so callers should be ready to star receiving packets.
     *
     * @param socketChannel   Socket to be wrapped.
     * @param isServerChannel Set to true if this is a server implementation e.g. not stopping the SelectorThread whne closing
     * @param selector        Selector to be used for managing IO events.
     * @param listener        Object to receive the callbacks.
     * @param protocolDecoder Decoder for reassembling the packets.
     * @throws IOException
     */
    public PacketChannel(
            SocketChannel socketChannel,
            boolean isServerChannel,
            SelectorThread selector,
            ProtocolDecoder protocolDecoder,
            PacketChannelListener listener) throws IOException
    {
        this.selector = selector;
        this.protocolDecoder = protocolDecoder;
        this.sc = socketChannel;
        this.isServerChannel = isServerChannel;

        this.sc.socket().setSendBufferSize(socketBufferSize);
        this.sc.socket().setReceiveBufferSize(socketBufferSize);

        this.listener = listener;
        this.remoteAddress = sc.socket().getRemoteSocketAddress().toString();
        this.remoteIPAddress = (((InetSocketAddress) sc.socket().getRemoteSocketAddress()).getAddress()).toString().replace("/","");

        // Registers with read interest.
        selector.registerChannel(sc, SelectionKey.OP_READ, this);

        //System.out.println(sc.socket().getReceiveBufferSize() + " : " + sc.socket().getSendBufferSize());
    }

    /**
     * Reactive reading on this channel
     *
     * @throws IOException
     */
    public final void reactivateReading() throws IOException
    {
        selector.addChannelInterest(sc, SelectionKey.OP_READ);
    }

    /**
     * Close the channel
     */
    public final void close()
    {
        try
        {
            synchronized (closedMutex)
            {
                if (closed)
                {
                    return;
                }

                closed = true;
            }

            if (!isServerChannel)
            {
                selector.requestClose();
            }

            Socket s = sc.socket();
            if (s != null)
            {
                try
                {
                    s.shutdownInput();
                }
                catch (Exception ex)
                {
                }
                try
                {
                    s.shutdownOutput();
                }
                catch (Exception ex)
                {
                }
            }
            sc.close();
        }
        catch (Exception e)
        {
            // Ignore
        }

        listener.socketDisconnected(this, !performed);

        performed = false;

    }

    /**
     * Reads from the socket into the internal buffer. This method should be
     * called only from the SelectorThread class.
     */

    public final void handleRead()
    {

        try
        {
//            inBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // Reads from the socket
            // Returns -1 if it has reached end-of-stream

            int readBytes = sc.read(inBuffer);
            //System.out.println("handle read");
/*
            if (logger.isLoggable(Level.FINER))
            {
                logger.finer("HANDLED_READ: " + readBytes);
            }
*/

            // End of stream???
            if (readBytes == -1)
            {
                // End of stream. Closing channel...
                close();
                return;
            }

            // Nothing else to be read?
            if (readBytes == 0)
            {
                return;
            }

            // There is some data in the buffer. Processes it.

            performed = true;

            inBuffer.flip();
            processInBuffer(inBuffer);

        }
        catch (IOException ex)
        {
            // Serious error. Close socket.
            close();
            listener.socketException(
                    this,
                    ex);
        }
        catch (InterruptedException e)
        {
            close();
            listener.socketException(
                    this,
                    e);
        }
    }


    /**
     * Processes the internal buffer, converting it into packets if enough data
     * is available.
     */
    private void processInBuffer(ByteBuffer inBuffer) throws InterruptedException
    {
        // prepare to read
        // inBuffer.flip();

        // basic raw dump
        // System.out.println("rawR:"+packetRawDump(inBuffer));
        for (; ; )
        {
            // System.out.println(MessageFormat.format("buf pos {0} limit {1}", inBuffer.position(), inBuffer.limit()));
            ByteBuffer packet = null;
            try
            {
                packet = protocolDecoder.decode(inBuffer);
            }
            catch (Exception ex)
            {
                Print.printStackTrace(ex, logger);
                close();
                return;
            }
            // A packet may or may not have been fully assembled, depending
            // on the data available in the buffer
            if (packet != null)
            {
                // debug only - check if the packet is well formated
                // System.out.println("rcv:pckt" + packetRawDump(packet));
                // A packet was reassembled.
                listener.packetArrived(this, packet);
                // The inBuffer might still have some data left. Perhaps
                // the beginning of another packet. So don't clear it. Next
                // time reading is activated, we start by processing the
                // inBuffer
                // again.

                // if the packet is not null mybe there is more packets in the
                // buffer.

            }
            else
            {
                // if the packet is null there is no more to process from this
                // inBuffer
                break;
            }

        }
        //compact and mark
        inBuffer.compact();
    }

    /**
     * Sends a packet using non-blocking writes. The caller must ensure that it
     * is not on the SelectorThread.
     * <p/>
     * This class keeps a reference to buffer given as argument while sending
     * it. So it is important not to change this buffer after calling this
     * method.
     *
     * @param packet The packet to be sent.
     */


    private long lastTimePrint = 0;
    private long lastTimeCheck = 0;
    private volatile long totalPackets = 0;
    private long lastPackets = 0;
    private final static long countCalc = 32768;

    public final void sendPacket(ByteBuffer packet)
    {
        totalPackets++;

        boolean packetInTheQ = false;
        // caching
        synchronized (cacheBufferMutex)
        {
            if (cacheBuffer == null)
            {
                logger.severe("CacheBuffer is null : Free Memory : " + (int) ((double) Runtime.getRuntime().freeMemory() / (1024D * 1024D)) + "m");
                close();
                return;
            }
            if (packet == null)
            {
                logger.severe("Packet is null : Free Memory : " + (int) ((double) Runtime.getRuntime().freeMemory() / (1024D * 1024D)) + "m");
                close();
                return;
            }
            if (cacheBuffer.remaining() < packet.remaining())
            {
                // no space in the buffer for this packet
                if (cacheBuffer.position() > 0)
                {
                    // there is data push it
                    try
                    {
                        cacheBuffer.flip();
                        outQueue.put(cacheBuffer);
                        packetInTheQ = true;
                    }
                    catch (Exception e)
                    {
                        Print.printStackTrace(e, logger);
                    }

                    cacheBuffer = cachePool.poll();
                    if (cacheBuffer == null)
                    {
                        // no elemnts in the pool available
                        cacheBuffer = ByteBuffer.allocate(socketBufferSize / cacheDenominator);
                    }

                    if (cacheBuffer.remaining() < packet.remaining())
                    {
                        // packet bigger then cachebuffer
                        try
                        {
                            outQueue.put(packet);
                            packetInTheQ = true;
                        }
                        catch (Exception e)
                        {
                            Print.printStackTrace(e, logger);
                        }
                    }
                    else
                    {
                        // there is still enough space in the cache buffer
                        cacheBuffer.put(packet);
                    }
                }
                else
                {
                    // packet bigger then cachebuffer
                    try
                    {
                        outQueue.put(packet);
                        packetInTheQ = true;
                    }
                    catch (Exception e)
                    {
                        Print.printStackTrace(e, logger);
                    }
                }
            }
            else
            {
                // there is still enough space in the cache buffer
                cacheBuffer.put(packet);
            }
        }

        if (packetInTheQ)
        {
            packetInTheOutQueue();
        }
        else
        {
            if (outQueue.size() == 0)
            {
                // make sure we do not wait a timeout is the q is empty
                pumpCache();
            }
        }
    }

    private void packetInTheOutQueue()
    {
        boolean processQSize = false;
        boolean checkQSize = false;

        synchronized (reactivateWritingMutex)
        {
            if ((totalPackets - lastPackets) > countCalc)
            {
                lastPackets = totalPackets;

                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastTimePrint) > (5 * 60 * 1000))  // 5 min
                {
                    processQSize = true;
                    lastTimePrint = currentTime;
                }

                if ((currentTime - lastTimeCheck) > (30 * 1000))  // 30 sec
                {
                    checkQSize = true;
                    lastTimeCheck = currentTime;
                }

                if (totalPackets < 0)
                {
                    totalPackets = 0;
                }
            }

            if (!isWritingScheduled)
            {
                isWritingScheduled = true;
                selector.invoke(reactivateWriting);
                selector.wakeup();
            }

        }

        // check q size
        if ((checkQSize) && (limitOutQueueSize > 0))
        {
            int qSize = outQueue.size();
            if (qSize > limitOutQueueSize)
            {
                //force check again immediately
                lastTimeCheck = 0;
                lastPackets = 0;
                if (selector.isSelectorThread())
                {
                    logger.severe("Slow consumer - queue too big. Drop Connection[" + remoteAddress + "] Outgoing Queue Size[" + qSize + "]");
                    close();
                }
            }
        }

        // logging Q size
        if (processQSize)
        {
            int pendingInvocationsSize = selector.pendingInvocationsSize();
            int qSize = outQueue.size();
            // if > 25K print with warining
            if (qSize < (limitOutQueueSize / 2))
            {
                logger.info("Connection[" + remoteAddress + "] Outgoing Queue Size[" + qSize + " : " + pendingInvocationsSize + "]");
            }
            else
            {
                logger.warning("Connection[" + remoteAddress + "] Outgoing Queue Size[" + qSize + " : " + pendingInvocationsSize + "]");
            }
        }
    }


    public final void pumpCache()
    {

        boolean packetInTheQ = false;

        synchronized (cacheBufferMutex)
        {
            if (cacheBuffer.position() > 0)
            {
                // there is data push it
                try
                {
                    cacheBuffer.flip();
                    outQueue.put(cacheBuffer);
                    packetInTheQ = true;
                }
                catch (Exception e)
                {
                    Print.printStackTrace(e, logger);
                }

                cacheBuffer = cachePool.poll();
                if (cacheBuffer == null)
                {
                    // no elemnts in the pool available
                    cacheBuffer = ByteBuffer.allocate(socketBufferSize / cacheDenominator);
                }
            }
        }

        if (packetInTheQ)
        {
            packetInTheOutQueue();
        }

    }

    /**
     * Writes to the underlying channel. Non-blocking. This method is called
     * only from sendPacket() and from the SelectorThread class.
     */
    public final void handleWrite(SelectionKey sk)
    {

        try
        {
            if (outBuffer == null)
            {
                /*
                try
                {
                    Thread.sleep(1);
                }
                catch (Exception ex)
                {
                    //not interested
                }
                */
                if (outQueue.size() == 0)
                {
                    // try once to pump
                    pumpCache();
                }

                synchronized (reactivateWritingMutex)
                {
                    if (outQueue.size() == 0)
                    {
                        // if Q still empty
                        // clear the write key
                        isWritingScheduled = false;
                        sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
                        return;
                    }
                }


                outBuffer = directBuffer;
                outBuffer.clear();

                int size = 0;
                for (int cnt = 0; (cnt < maxDrain) && outQueue.size() != 0; cnt++)
                {
                    int packetLen = outQueue.peek().remaining();
                    if ((size + packetLen) > socketBufferSize)
                    {
                        if (size != 0)
                        {
                            // send the existing data
                            break;
                        }
                        else
                        {
                            // packet to big to fit in one directbuffer
                            outBuffer = outQueue.poll();
                            break;
                        }
                    }
                    ByteBuffer cacheBuffer = outQueue.poll();
                    outBuffer.put(cacheBuffer);
                    size += packetLen;

                    // prepare and try put in the pool for reusing
                    cacheBuffer.clear();
                    // put in the pool only if created here, packet_buffer is not ready to be used as it can be in multiple Q's
                    if (cacheBuffer.remaining() == (socketBufferSize / cacheDenominator))
                    {
                        cachePool.offer(cacheBuffer);
                    }
                }

                // this is a outBuffer filled by us, in size == 0 case it si from the Q ready to be written
                if (size != 0)
                {
                    outBuffer.flip();
                }
            }
            int toBeWritten = outBuffer.remaining();
            if (toBeWritten == 0)
            {
                logger.severe("ByteBuffer for writing is empty!");
            }
            sc.write(outBuffer);
            if (toBeWritten != 0 && (toBeWritten == outBuffer.remaining()))
            {
                logger.severe("Nothing was actually written! Will close...");
                close();
            }
            performed = true;

            if (!outBuffer.hasRemaining())
            {
                // outBuffer was completly written. Notify waiting messageForwarder
                outBuffer = null;
            }

            // writing is active


        }
        catch (IOException ioe)
        {
            //ioe.printStackTrace();
            close();
            listener.socketException(
                    this,
                    ioe);
        }
    }

    /**
     * Return the underlining socket channel
     *
     * @return SocketChannel
     */
    public SocketChannel getSocketChannel()
    {
        return sc;
    }

    public String getRemoteAddress()
    {
        return remoteAddress;
    }

    public String getRemoteIPAddress()
    {
        return remoteIPAddress;
    }

    // debug routines
    /*
     * private boolean packetCheck(ByteBuffer bBuffer) { int possition =
     * bBuffer.position(); long size = OTS.getOTSSize(bBuffer); if (size !=
     * bBuffer.remaining()) { bBuffer.position(possition); return false; }
     * bBuffer.position(possition); return true; }
     *
     *
     * private String packetDump(ByteBuffer bBuffer) { int possition =
     * bBuffer.position(); long size = OTS.getOTSSize(bBuffer); String dump =
     * size + "["; for (int i = 0; i < size; i++) { if (bBuffer.hasRemaining()) {
     * dump = dump + bBuffer.get() + " "; } else { dump += " bytes missing.."; } }
     * dump += "]"; bBuffer.position(possition); return dump; }
     *
     * private String packetRawDump(ByteBuffer bBuffer) { int possition =
     * bBuffer.position(); String dump = bBuffer.remaining() + "["; while
     * (bBuffer.hasRemaining()) { if (bBuffer.hasRemaining()) { dump = dump +
     * bBuffer.get() + " "; } else { dump += " bytes missing.."; } } dump +=
     * "]"; bBuffer.position(possition); return dump; }
     */

    public SelectorThread getSelectorThread()
    {
        return selector;
    }
}
