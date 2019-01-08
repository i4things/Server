package net.b2net.utils.iot.server;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;

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
class StoreLast implements Serializable
{
    // oldest is last
    ArrayList<StoreElement> last = new ArrayList<StoreElement>();

    ArrayList<StoreElement> get()
    {
        return last;
    }

    StoreElement getAVG()
    {
        if (last.size() == 0)
        {
            return null;
        }
        ByteBuffer r = ByteBuffer.wrap(last.get(0).getData());
        r.order(Processor.ORDER);

        r.getShort(); // rssi
        int gateway_id = r.getShort() & 0xFFFF;
        int magic = r.getShort() & 0xFFFF;
        int site_id = r.getShort() & 0xFFFF;
        int device_id = r.getShort() & 0xFFFF;
        int secret_id = r.get() & 0xFF;
        long timestamp = last.get(0).getTimestamp();

        int rssi = 0;
        int volts_house = 0;
        int volts_start = 0;
        int degree_celsius = 0;
        int moisture_percent = 0;
        int water_percent = 0;

        for (int i = 0; i < last.size(); i++)
        {
            ByteBuffer a = ByteBuffer.wrap(last.get(i).getData());
            a.order(Processor.ORDER);

            rssi += a.getShort();

            a.getShort(); // gateway
            a.getShort();// magic
            a.getShort();// site id
            a.getShort();// device id
            a.get();//secret id

            volts_house += (a.get() & 0xFF);
            volts_start += (a.get() & 0xFF);
            degree_celsius += (a.get() & 0xFF);
            moisture_percent += (a.get() & 0xFF);
            water_percent += (a.get() & 0xFF);
        }

        rssi /= last.size();
        volts_house /= last.size();
        volts_start /= last.size();
        degree_celsius /= last.size();
        moisture_percent /= last.size();
        water_percent /= last.size();

        byte[] b_a = new byte[last.get(0).getData().length];
        ByteBuffer b = ByteBuffer.wrap(b_a);
        b.order(Processor.ORDER);
        b.putShort((short) rssi);
        b.putShort((short) gateway_id);
        b.putShort((short) (magic & 0xFFFF));
        b.putShort((short) (site_id & 0xFFFF));
        b.putShort((short) (device_id & 0xFFFF));
        b.put((byte) (secret_id & 0xFF));
        b.put((byte) (volts_house & 0xFF));
        b.put((byte) (volts_start & 0xFF));
        b.put((byte) (degree_celsius & 0xFF));
        b.put((byte) (moisture_percent & 0xFF));
        b.put((byte) (water_percent & 0xFF));

        return new StoreElement(b_a, timestamp);
    }
}
