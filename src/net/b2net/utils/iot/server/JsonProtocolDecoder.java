package net.b2net.utils.iot.server;

import java.io.IOException;
import java.nio.ByteBuffer;
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
class JsonProtocolDecoder implements net.b2net.utils.iot.nio.io.ProtocolDecoder
{
    private final static Logger logger = Logger.getLogger(JsonProtocolDecoder.class.getCanonicalName());
    private final static int maxLineSize = 2048;

    private ByteBuffer buffer = null;


    /**
     * Try to decode a message
     *
     * @param socketBuffer ByteBuffer
     * @return ByteBuffer
     */
    @Override
    public ByteBuffer decode(ByteBuffer socketBuffer) throws IOException
    {
        // Reads until the buffer is empty or until a packet
        // is fully reassembled.
        if (buffer == null)
        {
            if (socketBuffer.remaining() == 0)
            {
                return null;
            }

            buffer = ByteBuffer.allocate(maxLineSize);

        }

        for (; socketBuffer.hasRemaining(); )
        {
            int b = (int) (socketBuffer.get() & 0xFF);
            switch (b)
            {
                case '\r':
                    continue;
                case '\n':
                {
                    ByteBuffer ret = buffer;
                    ret.position(0);
                    buffer = null;

                    return ret;

                }
                default:
                {
                    if (buffer.remaining() > 0)
                    {
                        buffer.put((byte) (b & 0xFF));
                    }
                    else
                    {
                        String errorMsg = "Line exceeds  : " + maxLineSize;
                        logger.severe(errorMsg);
                        throw new IOException(errorMsg);
                    }
                }
            }
        }
        // No packet was reassembled. There is not enough data.

        return null;

    }
}
