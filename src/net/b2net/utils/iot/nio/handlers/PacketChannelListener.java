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

import java.nio.ByteBuffer;

/**
 * Callback interface for receiving events from a Connector.
 */
public interface PacketChannelListener
{

    /**
     * Called when a packet is fully reassembled.
     *
     * @param pc   The source of the event.
     * @param pckt The reassembled packet
     */
    public void packetArrived(PacketChannel pc, ByteBuffer pckt) throws InterruptedException;

    /**
     * Called when some error occurs while reading or writing to the socket.
     *
     * @param pc The source of the event.
     * @param ex The exception representing the error.
     */
    public void socketException(PacketChannel pc, Exception ex);

    /**
     * Called when the read operation reaches the end of stream. This means that
     * the socket was closed.
     *
     * @param pc The source of the event.
     */
    public void socketDisconnected(PacketChannel pc, boolean failed);
}
