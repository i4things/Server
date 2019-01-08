package net.b2net.utils.iot.server;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
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
public class Utils
{
    private static final Logger logger = Logger.getLogger(Utils.class.getCanonicalName());

    public static final ThreadLocal<DecimalFormat> df0 =
            new ThreadLocal<DecimalFormat>()
            {
                @Override
                protected DecimalFormat initialValue()
                {
                    DecimalFormat decimalFormat = new DecimalFormat();
                    decimalFormat.setMaximumFractionDigits(0);
                    decimalFormat.setGroupingUsed(false);
                    return decimalFormat;
                }
            };
    public static final ThreadLocal<DecimalFormat> df1 =
            new ThreadLocal<DecimalFormat>()
            {
                @Override
                protected DecimalFormat initialValue()
                {
                    DecimalFormat decimalFormat = new DecimalFormat();
                    decimalFormat.setMaximumFractionDigits(1);
                    decimalFormat.setGroupingUsed(false);
                    return decimalFormat;
                }
            };
    public static final ThreadLocal<DecimalFormat> df2 =
            new ThreadLocal<DecimalFormat>()
            {
                @Override
                protected DecimalFormat initialValue()
                {
                    DecimalFormat decimalFormat = new DecimalFormat();
                    decimalFormat.setMaximumFractionDigits(2);
                    decimalFormat.setGroupingUsed(false);
                    return decimalFormat;
                }
            };
    public static final ThreadLocal<DecimalFormat> df6 =
            new ThreadLocal<DecimalFormat>()
            {
                @Override
                protected DecimalFormat initialValue()
                {
                    DecimalFormat decimalFormat = new DecimalFormat();
                    decimalFormat.setMaximumFractionDigits(6);
                    decimalFormat.setGroupingUsed(false);
                    return decimalFormat;
                }
            };


    public static final String MC_ROOT_KEY = "T8N5A0EY10X4S786";

    // 30 minutes
    public static final long NOTIFICATION_TIMEOUT = 30 * 60 * 1000;
    // one minute
    public static final long CHALLENGE_TIMEOUT = 60 * 1000;
    //max message returned to heartbeat
    public static final int MAX_MSG_HEARTBEAT = 60;
    //notification packet max size
    // max notification packet size
    public static final int MAX_MSG_SIZE = 32;
    public static final int MAX_NOTIFY_SIZE = MAX_MSG_SIZE + 8 + 8 + 2 + 1;
    // maximum size of buffer returned to heartbeat message
    public static final int MAX_MSG_SIZE_HEARTBEAT = (MAX_MSG_HEARTBEAT * MAX_NOTIFY_SIZE) + (4 + 1);
    // maximum network buffer size returned to heartbeat message
    public static final int MAX_BUFFER_SIZE_HEARTBEAT = 1024;


    public static final int MIN_RSSI = -150;

    public static final String NA_TEXT = "---";

    public final static int MAX_HISTORY = 2 * 31;


    public static void putOTS(ByteBuffer otsBuffer, long otsSize)
    {


        if (otsSize < 0x40)
        {
            otsBuffer.put((byte) (otsSize << 2));

        }
        else if (otsSize < 0x4000)
        {
            otsBuffer.putShort((short) ((otsSize << 2) | 1));

        }
        else if (otsSize < 0x40000000)
        {
            otsBuffer.putInt((int) ((otsSize << 2) | 2));
        }
        else if (otsSize < 0x4000000000000000L)
        {
            otsBuffer.putLong(((((otsSize) << 2)) | 3));
        }

    }

    public static long getOTS(ByteBuffer otsBuffer)
    {
        int position = otsBuffer.position();
        byte firstByte = otsBuffer.get(position);

        switch (firstByte & 0x3)
        {
            case 0:
            {
                otsBuffer.position(position + 1);
                return (firstByte >> 2) & 0x3F;
            }
            case 1:
            {
                return (otsBuffer.getShort() >> 2) & 0x3FFF;
            }
            case 2:
            {
                return ((otsBuffer.getInt() >> 2) & 0x3FFFFFFF);
            }
            case 3:
            {
                return (otsBuffer.getLong() >> 2) & 0x3FFFFFFFFFFFFFFFL;
            }
        }

        return 0;
    }


    public static StoreElement getHistAVG(StoreDayIoT storeDayIoT)
    {
        if (storeDayIoT.get().size() == 0)
        {
            return null;
        }

        // find first with data
        int first_idx = 0;
        for (; first_idx < storeDayIoT.get().size(); first_idx++)
        {
            if (storeDayIoT.get().get(first_idx).getData().length > 0)
            {
                break;
            }
        }

        if (first_idx == storeDayIoT.get().size())
        {
            return null;
        }

        ByteBuffer pckt_first = ByteBuffer.wrap(storeDayIoT.get().get(first_idx).getData());
        pckt_first.order(Processor.ORDER);

        PacketDataWrapperGateway first = new PacketDataWrapperGateway(pckt_first);

        int magic = first.getMagic();
        long timestamp = storeDayIoT.get().get(0).getTimestamp();

        int packet_time = 0;
        int degree_celsius = 0;
        int moisture_percent = 0;

        int count = 0;

        for (int i = first_idx; i < storeDayIoT.get().size(); i++)
        {
            ByteBuffer pckt = ByteBuffer.wrap(storeDayIoT.get().get(i).getData());
            pckt.order(Processor.ORDER);

            if (pckt.hasRemaining())
            {

                count++;
                PacketDataWrapperGateway element = new PacketDataWrapperGateway(pckt);

                packet_time += element.getPacketTimeRaw();
                degree_celsius += element.getDegreeRaw();
                moisture_percent += element.getMoistureRaw();
            }
        }

        packet_time /= count;
        degree_celsius /= count;
        moisture_percent /= count;

        // byte[] b_a = new byte[last.get(0).getData().length];
        ByteBuffer b = ByteBuffer.allocate(10);//type+16bytes
        b.order(Processor.ORDER);
        b.putShort((short) first.getMagic());
        b.putInt(first.getSiteId());
        b.putShort((short) packet_time);
        b.put((byte) degree_celsius);
        b.put((byte) moisture_percent);
        b.flip();//get ready to read
        byte[] array = new byte[b.remaining()];
        b.get(array);
        StoreElement storeElement = new StoreElement(array, timestamp);


        return storeElement;
    }

    public static String toHex(final byte[] b)
    {
        String result = "";
        for (int i = 0; i < b.length; i++)
        {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result.toUpperCase();
    }

    public static byte[] fromHex(final String s)
    {
        if ((s.length() % 2) != 0)
        {
            return null;
        }

        final byte result[] = new byte[s.length() / 2];
        final char enc[] = s.toCharArray();
        for (int i = 0; i < enc.length; i += 2)
        {
            StringBuilder curr = new StringBuilder(2);
            curr.append(enc[i]).append(enc[i + 1]);
            result[i / 2] = (byte) Integer.parseInt(curr.toString(), 16);
        }
        return result;
    }

}
