package net.b2net.utils.iot.server;

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

import java.nio.ByteBuffer;

public class PacketDataWrapperGateway
{
    private int magic;
    private int site_id;
    private int packet_time_raw;
    private int degree_raw = -1;
    private int moisture_percent_raw = -1;

    public PacketDataWrapperGateway(ByteBuffer pckt)
    {

        if (pckt.remaining() < 10)
        {
            return;
        }
        magic = (int) (pckt.getShort() & 0xFFFF);
        site_id = pckt.getInt();

        packet_time_raw = ((int) pckt.getShort() & 0xFFFF);

        degree_raw = pckt.get() & 0xFF;
        moisture_percent_raw = pckt.get() & 0xFF;

    }

    public int getMagic()
    {
        return magic;
    }

    public int getSiteId()
    {
        return site_id;
    }

    public int getPacketTimeRaw()
    {
        return packet_time_raw;
    }

    public String getDegree(boolean fahrenheit)
    {
        if (degree_raw < 0)
        {
            return Utils.NA_TEXT;
        }

        float degree_celsius = -20.0f + (((float) degree_raw) * 0.3137f);

        if (fahrenheit)
        {
            return Utils.df0.get().format((9.0d / 5.0d * degree_celsius) + 32.0d);
        }
        else
        {
            return Utils.df1.get().format(degree_celsius);
        }
    }

    public String getMoisture_percent()
    {
        if (moisture_percent_raw < 0)
        {
            return Utils.NA_TEXT;
        }

        return new Integer(moisture_percent_raw & 0xFF).toString();
    }


    public int getDegreeRaw()
    {
        return degree_raw;
    }

    public int getMoistureRaw()
    {
        return moisture_percent_raw;
    }

}
