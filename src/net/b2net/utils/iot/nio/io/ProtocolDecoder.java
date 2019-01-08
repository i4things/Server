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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Defines the interface that should be implemented to support different
 * protocols.
 * <p/>
 * The PacketChannel class is protocol-agnostic: it knows only how to read and
 * write raw bytes from and to the underlying socket. This was done to make the
 * class general, allowing it to be used in any situation. In order to transform
 * the raw bytes into packets, a strategy design pattern is used. PacketChannel
 * instances have an associated ProtocolDecoder, which is used to process the
 * incoming data and transform it into discreet packets of the corresponding
 * protocol. It's the ProtocolDecoder instance that contains all knowledge about
 * the protocol used in the connection.
 */
public interface ProtocolDecoder
{

    /**
     * Uses the data in the buffer given as argument to reassemble a packet.
     * <p/>
     * The buffer contains an arbitrary amount of data (typically, all the data
     * that was available on the read buffer of the socket). This data may or
     * may not include a full packet. Implementations of this method must be
     * able to handle with both situations gracefully, by reading from the
     * buffer until one of the following happens:
     * <p/>
     * <ul>
     * <li> A full packet is reassembled.
     * <li> No more data is available in the buffer.
     * </ul>
     * <p/>
     * If a full packet is reassembled, that packet should be returned and the
     * buffer position should be left in the position after the end of the
     * packet. If there is not enough data in the buffer to reassemble a packet,
     * this method should store internally the partially reassembled packet and
     * return null. The next time it is called, it should continue reassembling
     * the packet.
     *
     * @param bBuffer The buffer containing the newly received data.
     * @return null if there is not enough data in the buffer to reassemble the
     *         packet or a full packet if it was possible to reassemble one.
     * @throws IOException If it is not possible to reassemble the packet because of
     *                     errors in it.
     */
    public ByteBuffer decode(ByteBuffer bBuffer) throws IOException;
}
