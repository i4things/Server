package net.b2net.utils.iot.server;

import net.b2net.utils.iot.nio.handlers.PacketChannel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
public class Processor
{
    private static final Logger logger = Logger.getLogger(Processor.class.getCanonicalName());

    private static final int MAGIC_IOT_VAL = 86;
    private static final int MAGIC_IOT_HEARTBEAT = 6;

    public static final ByteOrder ORDER = ByteOrder.LITTLE_ENDIAN;

    private final Store store;


    Processor(Store store)
    {
        this.store = store;
    }

    void sync(PacketChannel pc)
    {

        long time_uint32 = System.currentTimeMillis() & 0xFFFFFFFFL;

        ByteBuffer b = ByteBuffer.allocate(5);
        b.order(Processor.ORDER);
        b.put((byte) 4);
        b.putInt((int) time_uint32);
        b.flip();

        pc.sendPacket(b);
    }

    boolean process(ByteBuffer in, PacketChannel pc)
    {
        in.order(Processor.ORDER);

        int type = (int) (in.get() & 0xFF);

        //logger.info("[DEBUG] message received from: " + pc.getRemoteAddress().toString() + " type: " + type);

        if (type == 127)
        {
            // IoT heartbeat  info sync
            int pos = in.position();
            int magic = (int) (in.getShort() & 0xFFFF);
            if ((((byte) (magic >> 8)) & 0xFF) != MAGIC_IOT_HEARTBEAT)
            {
                // not our protocol
                logger.severe("Protocol mismatch : MAGIC[" + (((byte) magic) & 0xFF) + "]");
                return false;
            }

            long site_id = in.getInt() & 0xFFFFFFFFL;

            int packet_time = ((int) in.getShort() & 0xFFFF);

            in.position(pos);
            byte[] data = new byte[in.remaining()];
            in.get(data);

            ByteBuffer res = store.heartbeat(site_id, data);

            pc.sendPacket(res);

        }
        else if (type == 128)
        {
            // IoT data
            int magic = (int) (in.getShort() & 0xFFFF);
            if ((((byte) (magic >> 8)) & 0xFF) != MAGIC_IOT_VAL)
            {
                // not our protocol
                logger.severe("Protocol mismatch : MAGIC[" + (((byte) magic) & 0xFF) + "]");
                return false;
            }

            //split-check-rebuild-to-original
            short rssi_raw = in.getShort();
            long gateway_id = in.getInt() & 0xFFFFFFFFL;

            byte[] gateway_key = store.getDatabaseProvider().getDataRole().getGatewayNetworkKey(gateway_id);
            if (gateway_key == null)
            {
                // not found gateway
                logger.severe("Protocol mismatch : GATEWAY[" + gateway_id + "] do not exists.");
                return false;
            }
            byte[] rest = new byte[in.remaining()];
            in.get(rest);

            ByteBuffer crc_data = ByteBuffer.wrap(XXTEABin.xxteaDecrypt(rest, gateway_key));
            crc_data.order(Processor.ORDER);
            long crc = ((long) crc_data.getInt()) & 0xFFFFFFFFL;
            byte[] data = new byte[crc_data.remaining()];
            crc_data.get(data);

            if (crc != XXTEACRCBin.crc4(data))
            {
                // not our protocol
                logger.severe("Protocol mismatch : GATEWAY[" + gateway_id + "] sign invalid.");
                return false;
            }

            ByteBuffer pckt_chk = ByteBuffer.wrap(data);
            pckt_chk.order(Processor.ORDER);

            pckt_chk.get(); // crc4
            pckt_chk.get(); // seq
            Utils.getOTS(pckt_chk); // gateway_id // hard coded to 10 - e.g. destination
            long device_id = Utils.getOTS(pckt_chk); // device_id

            boolean process = true;

            if ((gateway_id & 1) != 0)
            {
                // this is private gateway
                if (!store.getDatabaseProvider().getDataRole().matchGatewayNode(gateway_id, device_id))
                {
                    process = false;
                }

            }
            else if (!store.getDatabaseProvider().getDataRole().isNodeValid(device_id))
            {

                process = false;

            }

            sync(pc);

            if (process)
            {
                byte[] decoded = new byte[6 + data.length];
                ByteBuffer pckt = ByteBuffer.wrap(decoded);
                pckt.order(Processor.ORDER);
                pckt.putShort(rssi_raw);
                pckt.putInt((int) gateway_id);
                pckt.put(data);
                pckt.position(0);

                store.add_iot(pckt);
            }

        }
        else
        {
            return false;
        }

        return true;
    }


}
