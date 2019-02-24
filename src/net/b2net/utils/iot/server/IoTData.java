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
public class IoTData
{
    private long thing;
    private long timestamp;
    private double lon;
    private double lat;
    private byte rssi;
    private byte[] data;

    public final void setThing(long thing)
    {
        this.thing = thing;
    }

    public final void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

    public final void setLon(double lon)
    {
        this.lon = lon;
    }

    public final void setLat(double lat)
    {
        this.lat = lat;
    }

    public final void setRssi(byte rssi)
    {
        this.rssi = rssi;
    }

    public final void setData(byte[] data)
    {
        this.data = data;
    }

    public final long getTimestamp()
    {
        return timestamp;
    }

    public final double getLon()
    {
        return lon;
    }

    public final double getLat()
    {
        return lat;
    }

    public final byte getRssi()
    {
        return rssi;
    }

    public final byte[] getData()
    {
        return data;
    }

    public final long getThing()
    {
        return thing;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("var iot_json = '{");
        sb.append("\"thing\": ").append(thing).append(",");
        sb.append("\"last\": [");
        sb.append("{");
        sb.append("\"t\": ").append(timestamp).append(", ");
        sb.append("\"l\": ").append(Utils.df6.get().format(lat)).append(", ");
        sb.append("\"n\": ").append(Utils.df6.get().format(lon)).append(", ");
        sb.append("\"r\": ").append(rssi).append(", ");

        sb.append("\"d\": ").append("[ ");
        for (int i = 0; i < data.length; i++)
        {
            sb.append(Integer.toString(data[i] & 0xFF));
            if (i < (data.length - 1))
            {
                sb.append(", ");
            }
        }
        sb.append("] }");

        sb.append("]");
        sb.append("}';\n");

        return sb.toString();
    }
}
