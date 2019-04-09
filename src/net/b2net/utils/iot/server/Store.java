package net.b2net.utils.iot.server;

import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.nio.handlers.PacketChannel;
import net.b2net.utils.iot.server.storage.DatabaseProvider;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
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
class Store
{
    private static final Logger logger = Logger.getLogger(Store.class.getCanonicalName());


    private final GatewayBuffer gatewayBuffer = new GatewayBuffer();

    private final DatabaseProvider databaseProvider;
    private final StreamingProvider streamingProvider;

    private final ReadWriteLock lock;


    Store(final StreamingProvider streamingProvider,
          final DatabaseProvider databaseProvider,
          final ReadWriteLock lock)
    {
        this.databaseProvider = databaseProvider;
        this.streamingProvider = streamingProvider;
        this.lock = lock;
    }


    DatabaseProvider getDatabaseProvider()
    {
        return databaseProvider;
    }

    ByteBuffer heartbeat(final long site_id, final byte[] data)
    {
        lock.writeLock().lock();
        try
        {
            StoreTupleIoT storeTuple = databaseProvider.getStoreData().getGateways().get(site_id);
            if (storeTuple == null)
            {
                storeTuple = new StoreTupleIoT();
                databaseProvider.getStoreData().getGateways().put(site_id, storeTuple);
            }

            long now = System.currentTimeMillis();

            // check if new day
            if ((storeTuple.getLast().get().size() > 0) && (Store.getDaysBetween(now, storeTuple.getLast().get().get(0).getTimestamp()) != 0))
            {
                StoreDayIoT storeDayIoT = storeTuple.getLast();
                StoreDayIoT storeDayIoTHistory = new StoreDayIoT();
                storeDayIoTHistory.get().addAll(storeDayIoT.get());

                storeTuple.getHist().add(storeDayIoTHistory);
                storeDayIoT.get().clear();
            }

            storeTuple.getLast().get().add(0, new StoreElement(data, now));
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
        }
        finally
        {
            lock.writeLock().unlock();
        }

        ByteBuffer ret = ByteBuffer.allocate(Utils.MAX_MSG_SIZE_HEARTBEAT);
        ret.order(Processor.ORDER);
        ret.put((byte) 4);
        ret.putInt((int) (System.currentTimeMillis() & 0xFFFFFFFFL));

        gatewayBuffer.get(ret, site_id);

        ret.flip();

        return ret;
    }

    void add_iot(final ByteBuffer pckt)
    {

        int data_size = pckt.remaining();

        pckt.getShort(); // rssi
        pckt.getInt(); // gateway_id
        pckt.get(); // crc4
        pckt.get(); // seq
        Utils.getOTS(pckt); //// gateway_id // hard coded to 10 - e.g. destination
        //  int site_id = IOT_SITE_ID; // hard coded for all IoT
        Long device_id = Utils.getOTS(pckt);
        lock.writeLock().lock();
        try
        {
            StoreTupleIoT storeTuple = databaseProvider.getStoreData().getStore().get(device_id);
            if (storeTuple == null)
            {
                storeTuple = new StoreTupleIoT();
                databaseProvider.getStoreData().getStore().put(device_id, storeTuple);
            }

            long now = System.currentTimeMillis();

            // check if new day
            if ((storeTuple.getLast().get().size() > 0) && (Store.getDaysBetween(now, storeTuple.getLast().get().get(0).getTimestamp()) != 0))
            {
                StoreDayIoT storeDayIoT = storeTuple.getLast();
                StoreDayIoT storeDayIoTHistory = new StoreDayIoT();
                storeDayIoTHistory.get().addAll(storeDayIoT.get());

                storeTuple.getHist().add(storeDayIoTHistory);
                storeDayIoT.get().clear();
            }

            //and finally add to last
            byte[] data = new byte[data_size];
            pckt.position(0);
            pckt.get(data);
            storeTuple.getLast().get().add(0, new StoreElement(data, now));

            if (streamingProvider != null)
            {
                if (streamingProvider.isSubscribed(device_id.longValue()))
                {
                    IoTData raw_data = new IoTData();
                    extract_data(device_id.longValue(), true, 0, raw_data, new AtomicBoolean(false));
                    streamingProvider.dataReceived(databaseProvider.getDataRole().getAccount(device_id.longValue()), raw_data);
                }
            }

        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }


    private static byte[] getDataFromGroup_iot(ArrayList<StoreElement> eq_data)
    {
        byte[] data = null;
        short max_rssi = Short.MIN_VALUE;
        for (int i = 0; i < eq_data.size(); i++)
        {
            StoreElement l = eq_data.get(i);
            ByteBuffer pckt = ByteBuffer.wrap(l.getData());
            pckt.order(Processor.ORDER);

            short raw_rssi = pckt.getShort();
            if (raw_rssi > max_rssi)
            {
                max_rssi = raw_rssi;
                data = eq_data.get(i).getData();
            }

        }
        return data;
    }

    private static long getTimestampFromGroup_iot(ArrayList<StoreElement> eq_data)
    {
        return eq_data.get(0).getTimestamp();
    }

    private double[] getPosFromGroup_iot(ArrayList<StoreElement> eq_data)
    {
        double[] ret = null;
        ArrayList<double[]> gateway_pos = new ArrayList<double[]>();
        for (int i = 0; i < eq_data.size(); i++)
        {
            StoreElement l = eq_data.get(i);
            ByteBuffer pckt = ByteBuffer.wrap(l.getData());
            pckt.order(Processor.ORDER);

            int raw_rssi = pckt.getShort();
            long gateway_id = pckt.getInt() & 0xFFFFFFFFL; // gateway_id

            double[] pos = databaseProvider.getDataRole().getGatewayPos(gateway_id);
            if (pos != null)
            {
                gateway_pos.add(new double[]{pos[0], pos[1], (double) raw_rssi});
            }

        }

        if (gateway_pos.size() > 0)
        {
            ret = getPosition(gateway_pos);
        }

        return ret;
    }


    String get_iot_hist(final long device_id, final int dey_idx, String challenge, AtomicBoolean err)
    {
        //check challenge

        byte[] network_key = databaseProvider.getDataRole().getNodeNetworkKey(device_id);
        if (network_key == null)
        {

            logger.severe("Protocol mismatch : NODE[" + device_id + "] not found.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"node not found\" }';\n";

        }

        byte[] challenge_arr = Utils.fromHex(challenge);
        if (challenge_arr == null)
        {
            err.set(true); return "var iot_json = '{ \"ERR\" : \"wrong format\" }';\n";
        }

        ByteBuffer crc_data = ByteBuffer.wrap(XXTEABin.xxteaDecrypt(challenge_arr, network_key));
        crc_data.order(Processor.ORDER);
        long crc = crc_data.getInt() & 0xFFFFFFFFL;
        int timestamp_pos = crc_data.position();
        byte[] timestamp_arr = new byte[crc_data.remaining()];
        crc_data.get(timestamp_arr);

        if (crc != XXTEACRCBin.crc4(timestamp_arr))
        {
            // not our protocol
            logger.severe("Protocol mismatch : sign invalid.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"sign invalid\" }';\n";
        }

        crc_data.position(timestamp_pos);
        long timestamp_utc = crc_data.getLong();

        if ((System.currentTimeMillis() - timestamp_utc) > Utils.CHALLENGE_TIMEOUT)
        {
            // not our protocol
            logger.severe("Protocol mismatch : challenge timeout.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"timeout\" }';\n";
        }

        // all good and valid continue

        StringBuilder sb = new StringBuilder();

        lock.readLock().lock();
        try
        {
            sb.append("var iot_json = '{");
            sb.append("\"thing\": ").append(device_id).append(",");
            sb.append("\"hist\": ").append(dey_idx).append(",");
            StoreTupleIoT storeTuple = databaseProvider.getStoreData().getStore().get(new Long(device_id));


            sb.append("\"day\": [");
            if (storeTuple != null)
            {

                ArrayList<StoreElement> eq_data = new ArrayList<StoreElement>();
                long last_updated = 0;
                int last_seq = 0;

                if (storeTuple.getHist().get().size() > dey_idx)
                {
                    StoreDayIoT storeDay = storeTuple.getHist().get().get(dey_idx);

                    for (int i = 0; i < storeDay.get().size(); i++)
                    {
                        StoreElement l = storeDay.get().get(i);

                        if (last_updated == 0)
                        {
                            last_updated = l.getTimestamp();
                        }
                        // test if the same data
                        ByteBuffer chk = ByteBuffer.wrap(l.getData());
                        chk.order(Processor.ORDER);
                        chk.getShort();  // rssi
                        chk.getInt(); // gateway_id
                        chk.get(); // crc4
                        int seq = chk.get() & 0xFF;

                        boolean last_eq = false;
                        if ((last_seq == 0) || (last_seq == seq))
                        {
                            last_seq = seq;
                            eq_data.add(l);

                            if (i != (storeDay.get().size() - 1))
                            {
                                continue;
                            }

                            last_eq = true;
                        }

                        for (; ; )
                        {
                            byte[] data = getDataFromGroup_iot(eq_data);
                            long timestamp = getTimestampFromGroup_iot(eq_data);
                            double[] pos = getPosFromGroup_iot(eq_data);

                            eq_data.clear();
                            eq_data.add(l);
                            last_seq = seq;


                            sb.append("{");
                            sb.append("\"t\": ").append(timestamp).append(", ");
                            if (pos != null)
                            {
                                sb.append("\"l\": ").append(Utils.df6.get().format(pos[0])).append(", ");
                                sb.append("\"n\": ").append(Utils.df6.get().format(pos[1])).append(", ");
                            }
                            ByteBuffer pckt = ByteBuffer.wrap(data);
                            pckt.order(Processor.ORDER);

                            int raw_rssi = pckt.getShort();
                            int rssi = ((raw_rssi - Utils.MIN_RSSI) * 100) / (Utils.MIN_RSSI * (-1));
                            if (rssi < 0)
                            {
                                rssi = 0;
                            }
                            else if (rssi > 100)
                            {
                                rssi = 100;
                            }
                            long gateway_id = pckt.getInt() & 0xFFFFFFFFL; // gateway_id
                            pckt.get(); // crc4
                            pckt.get(); // seq
                            Utils.getOTS(pckt); // gateway_id // hard coded to 10 - e.g. destination
                            Utils.getOTS(pckt); // device_id

                            sb.append("\"r\": ").append(rssi).append(", ");
                            sb.append("\"d\": ").append("[ ");
                            for (; pckt.hasRemaining(); )
                            {
                                sb.append(Integer.toString(pckt.get() & 0xFF));
                                if (pckt.hasRemaining())
                                {
                                    sb.append(", ");
                                }
                            }
                            sb.append("] },");


                            if ((i == (storeDay.get().size() - 1)) && (!last_eq))
                            {
                                // last one need processing too only in case it is different from the group
                                // if from the same group it is already processed
                                // <this part is here because we have already collected the last one and need to process it also>
                                last_eq = true;   // and make sure do not enter again in endless look
                                continue;
                            }

                            break;
                        }
                    }

                    if (storeDay.get().size() > 0)
                    {
                        sb.setLength(sb.length() - 1);
                    }
                }
            }
            sb.append("]");

            sb.append("}';\n");

            return sb.toString();
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "var iot_json = '{ \"ERR\" : \"exception\" }';\n";
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    // if iot_data != null and top is true then we return null and get teh binary format only
    String extract_data(final long device_id, final boolean top, final long from, IoTData iot_data, AtomicBoolean err)
    {
        StringBuilder sb = new StringBuilder();
        String ret = null;

        lock.readLock().lock();
        try
        {

            if (top && (iot_data != null))
            {
                iot_data.setThing(device_id);
            }
            else
            {
                sb.append("var iot_json = '{");
                sb.append("\"thing\": ").append(device_id).append(",");
            }


            StoreTupleIoT storeTuple = databaseProvider.getStoreData().getStore().get(new Long(device_id));

            if (top && (iot_data != null))
            {
                // nothing to do in binary format
            }
            else
            {
                sb.append("\"last\": [");
            }
            if (storeTuple != null)
            {

                ArrayList<StoreElement> eq_data = new ArrayList<StoreElement>();
                long last_updated = 0;
                int last_seq = 0;

                // user together with top to exit te loop
                boolean get_out = false;

                for (int i = 0; (i < storeTuple.getLast().get().size()) && (!get_out); i++)
                {
                    StoreElement l = storeTuple.getLast().get().get(i);


                    // process the element
                    if (last_updated == 0)
                    {
                        last_updated = l.getTimestamp();
                    }
                    // test if the same data
                    ByteBuffer chk = ByteBuffer.wrap(l.getData());
                    chk.order(Processor.ORDER);
                    chk.getShort();  // rssi
                    chk.getInt(); // gateway_id
                    chk.get(); // crc4
                    int seq = chk.get() & 0xFF;

                    boolean last_eq = false;
                    if ((last_seq == 0) || (last_seq == seq))
                    {
                        last_seq = seq;
                        eq_data.add(l);

                        if (i != (storeTuple.getLast().get().size() - 1))
                        {
                            continue;
                        }

                        last_eq = true;
                    }


                    for (; ; )
                    {
                        byte[] data = getDataFromGroup_iot(eq_data);
                        long timestamp = getTimestampFromGroup_iot(eq_data);
                        double[] pos = getPosFromGroup_iot(eq_data);

                        eq_data.clear();
                        eq_data.add(l);
                        last_seq = seq;

                        // check if not wanted by timestamp
                        if (timestamp <= from)
                        {
                            // too old and all next are old also - do not process and get out
                            get_out = true;
                            break;
                        }

                        if (top && (iot_data != null))
                        {
                            iot_data.setTimestamp(timestamp);
                            iot_data.setLat(pos[0]);
                            iot_data.setLon(pos[1]);
                        }
                        else
                        {

                            sb.append("{");
                            sb.append("\"t\": ").append(timestamp).append(", ");
                            if (pos != null)
                            {
                                sb.append("\"l\": ").append(Utils.df6.get().format(pos[0])).append(", ");
                                sb.append("\"n\": ").append(Utils.df6.get().format(pos[1])).append(", ");
                            }
                        }


                        ByteBuffer pckt = ByteBuffer.wrap(data);
                        pckt.order(Processor.ORDER);

                        int raw_rssi = pckt.getShort();
                        int rssi = ((raw_rssi - Utils.MIN_RSSI) * 100) / (Utils.MIN_RSSI * (-1));
                        if (rssi < 0)
                        {
                            rssi = 0;
                        }
                        else if (rssi > 100)
                        {
                            rssi = 100;
                        }
                        long gateway_id = pckt.getInt() & 0xFFFFFFFFL; // gateway_id
                        pckt.get(); // crc4
                        pckt.get(); // seq
                        Utils.getOTS(pckt); // gateway_id // hard coded to 10 - e.g. destination
                        Utils.getOTS(pckt); // device_id

                        if (top && (iot_data != null))
                        {
                            iot_data.setRssi((byte) rssi);
                            iot_data.setLat(pos[0]);
                            iot_data.setLon(pos[1]);
                            byte[] raw_data = new byte[pckt.remaining()];
                            pckt.get(raw_data);
                            iot_data.setData(raw_data);
                        }
                        else
                        {
                            sb.append("\"r\": ").append(rssi).append(", ");
                            sb.append("\"d\": ").append("[ ");

                            for (; pckt.hasRemaining(); )
                            {
                                sb.append(Integer.toString(pckt.get() & 0xFF));
                                if (pckt.hasRemaining())
                                {
                                    sb.append(", ");
                                }
                            }
                            sb.append("] },");
                        }

                        if (top)
                        {
                            // we need only the top element;
                            get_out = true;
                            break;
                        }

                        if ((i == (storeTuple.getLast().get().size() - 1)) && (!last_eq))
                        {
                            // last one need processing too only in case it is different from the group
                            // if from the same group it is already processed
                            // <this part is here because we have already collected the last one and need to process it also>
                            last_eq = true;   // and make sure do not enter again in endless look
                            continue;
                        }

                        break;
                    }
                }

                if (top && (iot_data != null))
                {
                    // nothing to do in binary format
                }
                else
                {
                    if (storeTuple.getLast().get().size() > 0)
                    {
                        sb.setLength(sb.length() - 1);
                    }
                }
            }
            if (top && (iot_data != null))
            {
                // nothing to do in binary format
            }
            else
            {
                sb.append("]");
                sb.append("}';\n");
                ret = sb.toString();
            }

            return ret;
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "var iot_json = '{ \"ERR\" : \"exception\" }';\n";
        }
        finally
        {
            lock.readLock().unlock();
        }
    }


    String get_iot_get(final long device_id, final String challenge, final boolean top, final long from, AtomicBoolean err)
    {
        //check challenge

        byte[] network_key = databaseProvider.getDataRole().getNodeNetworkKey(device_id);
        if (network_key == null)
        {
            logger.severe("Protocol mismatch : NODE[" + device_id + "] not found.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"node not found\" }';\n";
        }

        byte[] challenge_arr = Utils.fromHex(challenge);
        if (challenge_arr == null)
        {
            err.set(true); return "var iot_json = '{ \"ERR\" : \"wrong format\" }';\n";
        }

        ByteBuffer crc_data = ByteBuffer.wrap(XXTEABin.xxteaDecrypt(challenge_arr, network_key));
        crc_data.order(Processor.ORDER);
        long crc = crc_data.getInt() & 0xFFFFFFFFL;
        int timestamp_pos = crc_data.position();
        byte[] timestamp_arr = new byte[crc_data.remaining()];
        crc_data.get(timestamp_arr);

        if (crc != XXTEACRCBin.crc4(timestamp_arr))
        {
            // not our protocol
            logger.severe("Protocol mismatch : sign invalid.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"sign invalid\" }';\n";
        }

        crc_data.position(timestamp_pos);
        long timestamp_utc = crc_data.getLong();

        if ((System.currentTimeMillis() - timestamp_utc) > Utils.CHALLENGE_TIMEOUT)
        {
            // not our protocol
            logger.severe("Protocol mismatch : challenge timeout.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"timeout\" }';\n";
        }

        // all good and valid continue

        return extract_data(device_id, top, from, null, new AtomicBoolean(false));
    }


    private long getGatewayId(StoreDayIoT storeDayIoT)
    {
        ArrayList<StoreElement> eq_data = new ArrayList<StoreElement>();
        int last_seq = 0;

        for (int i = 0; i < storeDayIoT.get().size(); i++)
        {
            StoreElement l = storeDayIoT.get().get(i);

            // test if the same data
            ByteBuffer chk = ByteBuffer.wrap(l.getData());
            chk.order(Processor.ORDER);
            chk.getShort();  // rssi
            chk.getInt(); // gateway_id
            chk.get(); // crc
            int seq = chk.get() & 0xFF;

            if ((last_seq == 0) || (last_seq == seq))
            {
                last_seq = seq;
                eq_data.add(l);

                if (i != (storeDayIoT.get().size() - 1))
                {
                    continue;
                }

            }

            byte[] data = getDataFromGroup_iot(eq_data);

            ByteBuffer pckt = ByteBuffer.wrap(data);
            pckt.order(Processor.ORDER);

            int raw_rssi = pckt.getShort();
            return (pckt.getInt() & 0xFFFFFFFFL); // gateway_id
        }

        return -1;
    }


    String get_iot_set(final long device_id, final String challenge_hex, final String data_hex, AtomicBoolean err)
    {
        byte[] network_key = databaseProvider.getDataRole().getNodeNetworkKey(device_id);
        if (network_key == null)
        {
            logger.severe("Protocol mismatch : NODE[" + device_id + "] not found.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"node not found\" }';\n";
        }

        byte[] challenge_arr = Utils.fromHex(challenge_hex);
        if (challenge_arr == null)
        {
            err.set(true); return "var iot_json = '{ \"ERR\" : \"wrong format\" }';\n";
        }

        ByteBuffer crc_data = ByteBuffer.wrap(XXTEABin.xxteaDecrypt(challenge_arr, network_key));
        crc_data.order(Processor.ORDER);
        long crc = crc_data.getInt() & 0xFFFFFFFFL;
        int timestamp_pos = crc_data.position();
        byte[] timestamp_arr = new byte[crc_data.remaining()];
        crc_data.get(timestamp_arr);

        if (crc != XXTEACRCBin.crc4(timestamp_arr))
        {
            // not our protocol
            logger.severe("Protocol mismatch : sign invalid.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"sign invalid\" }';\n";
        }

        crc_data.position(timestamp_pos);
        long timestamp_utc = crc_data.getLong();

        if ((System.currentTimeMillis() - timestamp_utc) > Utils.CHALLENGE_TIMEOUT)
        {
            // not our protocol
            logger.severe("Protocol mismatch : challenge timeout.");
            err.set(true); return "var iot_json = '{ \"ERR\" : \"timeout\" }';\n";
        }


        byte[] data_arr = Utils.fromHex(data_hex);

        if (data_arr.length > Utils.MAX_MSG_SIZE)
        {
            // not our protocol
            logger.severe("Protocol mismatch : message too big : " + data_arr.length);
            err.set(true); return "var iot_json = '{ \"ERR\" : \"message too big\" }';\n";
        }

        // all good and valid continue

        lock.readLock().lock();
        try
        {
            StoreTupleIoT storeTuple = databaseProvider.getStoreData().getStore().get(new Long(device_id));
            if (storeTuple != null)
            {
                long gateway_id = getGatewayId(storeTuple.getLast());
                if (gateway_id == -1)
                {
                    for (int i = 0; i < storeTuple.getHist().get().size(); i++)
                    {
                        gateway_id = getGatewayId(storeTuple.getHist().get().get(i));
                        if (gateway_id != -1)
                        {
                            break;
                        }
                    }
                }

                if (gateway_id != -1)
                {
                    // make sure the message is not empty which will guaranty that the message size will be > 5 ( length = 4)
                    // and will not be treated as a sycn
                    if ((data_arr != null) && (data_arr.length > 0))
                    {
                        // make a ready to be sent packet - only seq and crc need to be set
                        byte[] buf = new byte[data_arr.length + 1 + 1 + 1 + 8 + 8];
                        ByteBuffer to_gateway = ByteBuffer.wrap(buf);
                        to_gateway.order(Processor.ORDER);
                        to_gateway.put((byte) 0); // SIZE
                        to_gateway.put((byte) 0); // CRC
                        to_gateway.put((byte) 0); // SEQ
                        Utils.putOTS(to_gateway, device_id); // destination id
                        Utils.putOTS(to_gateway, gateway_id); // source id
                        to_gateway.put(data_arr);

                        // reset for reading
                        to_gateway.flip();
                        // set size directly in the underline array - exclude the size byte
                        buf[0] = (byte) (to_gateway.remaining() - 1);

                        gatewayBuffer.add(gateway_id, to_gateway);

                        return "var iot_json = '{ \"RES\" : \"OK\" }';";
                    }

                    err.set(true); return "var iot_json = '{ \"ERR\" : \"message empty\" }';";

                }

            }

            err.set(true); return "var iot_json = '{ \"ERR\" : \"no data from node\" }';";

        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "var iot_json = '{ \"ERR\" : \"exception\" }';\n";
        }
        finally
        {
            lock.readLock().unlock();
        }

    }


    private static int getDaysBetween(long start, long end)
    {

        boolean negative = false;
        if (start > end)
        {
            negative = true;
            long temp = start;
            start = end;
            end = temp;
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(start);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        GregorianCalendar calEnd = new GregorianCalendar();
        calEnd.setTimeInMillis(end);
        calEnd.set(Calendar.HOUR_OF_DAY, 0);
        calEnd.set(Calendar.MINUTE, 0);
        calEnd.set(Calendar.SECOND, 0);
        calEnd.set(Calendar.MILLISECOND, 0);


        if (cal.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR))
        {
            if (negative)
            {
                return (calEnd.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR)) * -1;
            }
            return calEnd.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR);
        }

        int days = 0;
        while (calEnd.after(cal))
        {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            days++;
        }
        if (negative)
        {
            return days * -1;
        }
        return days;
    }

    // double[] { Lon, Lat, Strength } degrees, db
    private static double[] getPosition(ArrayList<double[]> coordinates)
    {
        if (coordinates.size() == 1)
        {
            return new double[]{coordinates.get(0)[0], coordinates.get(0)[1]};
        }

        double lat = 0.0d;
        double lon = 0.0d;

        double total_strength = 0.0d;

        for (int i = 0; i < coordinates.size(); i++)
        {
            total_strength += coordinates.get(i)[2];
        }

        for (int i = 0; i < coordinates.size(); i++)
        {
            double strength = coordinates.get(i)[2] / total_strength;
            lat += coordinates.get(i)[0] * strength;
            lon += coordinates.get(i)[1] * strength;
        }

        return new double[]{lat, lon};
    }

    String get_mc_reg_facilitator(String payload, AtomicBoolean err)
    {
        try
        {
            String crc_key_str_guid = XXTEAString.decryptBase64StringToString(payload, Utils.MC_ROOT_KEY);
            //          1         2         3         4         5         6         7
            //012345678901234567890123456789012345678901234567890123456789012345678901234
            //FFFF516210150A0D2E171E0B510B0F101626{9C0A620B-3400-4F5D-87DD-D4133C4D4C52}MYFACILITATOR
            if (crc_key_str_guid.length() < 75)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_key_str_guid.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_key_str_guid.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_key = crc_key_str_guid.substring(4, 36);
            byte[] facilitatorKey = Utils.fromHex(str_key);
            if (facilitatorKey == null)
            {
                err.set(true); return "WRNG5[KEY]";
            }
            String str_guid = crc_key_str_guid.substring(37, 73);
            UUID facilitatorId = UUID.fromString(str_guid);
            if (!databaseProvider.getDataRole().addFacilitator(facilitatorId, facilitatorKey, crc_key_str_guid.substring(74)))
            {
                err.set(true); return "WRNG6[DUP]";
            }
            return "OK";
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_reg_account(String str_falilitatorId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_falilitatorId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_falilitatorId + "]";
            }
            UUID facilitatorId = UUID.fromString(str_falilitatorId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getFacilitatorNetworkKey(facilitatorId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_key_str_guid = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3         4         5         6         7
            //012345678901234567890123456789012345678901234567890123456789012345678901234
            //FFFF516210150A0D2E171E0B510B0F101626{9C0A620B-3400-4F5D-87DD-D4133C4D4C52}MYACCOUNT
            if (crc_key_str_guid.length() < 75)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_key_str_guid.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_key_str_guid.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_key = crc_key_str_guid.substring(4, 36);
            byte[] accountKey = Utils.fromHex(str_key);
            if (accountKey == null)
            {
                err.set(true); return "WRNG5[KEY]";
            }
            String str_guid = crc_key_str_guid.substring(37, 73);
            UUID accountId = UUID.fromString(str_guid);
            if (!databaseProvider.getDataRole().addAccount(accountId, facilitatorId, accountKey, crc_key_str_guid.substring(74)))
            {
                err.set(true); return "WRNG6[DUP]";
            }
            return "OK";
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_reg_node(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_key = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //0123456789012345678901234567890123456
            //FFFF516210150A0D2E171E0B510B0F101626MYNODE
            if (crc_key.length() < 37)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_key.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_key.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_key = crc_key.substring(4, 36);
            byte[] nodeKey = Utils.fromHex(str_key);
            if (nodeKey == null)
            {
                err.set(true); return "WRNG5[KEY]";
            }
            Long nodeId = databaseProvider.getDataRole().addNode(accountId, nodeKey, crc_key.substring(36));
            if (nodeId == null)
            {
                err.set(true); return "WRNG6[DUP]";
            }
            return nodeId.toString();
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_reg_gateway(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_key_pos = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //01234567890123456789012345678901234567
            //FFFF516210150A0D2E171E0B510B0F101626#MYGATEWAY#51.438939#-0.218631#1
            if (crc_key_pos.length() < 36)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_key_pos.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_key_pos.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_key = crc_key_pos.substring(4, 36);
            byte[] gatewayKey = Utils.fromHex(str_key);
            if (gatewayKey == null)
            {
                err.set(true); return "WRNG5[KEY]";
            }
            String[] pos_open = crc_key_pos.substring(37).split("#");
            Long gatewayId = databaseProvider.getDataRole().addGateway(accountId, gatewayKey, Double.parseDouble(pos_open[1]), Double.parseDouble(pos_open[2]), (pos_open[3].compareTo("1") == 0), pos_open[0]);
            if (gatewayId == null)
            {
                err.set(true); return "WRNG6[DUP]";
            }
            return gatewayId.toString();
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_del_gateway(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //012345678901234567890123456789012345
            //FFFF5136
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_id = crc_id.substring(4);

            if (!databaseProvider.getDataRole().removeGateway(Long.parseLong(str_id)))
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return "OK";
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_del_node(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //012345678901234567890123456789012345
            //FFFF5136
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_id = crc_id.substring(4);

            if (!databaseProvider.getDataRole().removeNode(Long.parseLong(str_id)))
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return "OK";
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_del_account(String str_facilitatorId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_facilitatorId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_facilitatorId + "]";
            }
            UUID facilitatorId = UUID.fromString(str_facilitatorId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getFacilitatorNetworkKey(facilitatorId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF{9C0A620B-3400-4F5D-87DD-D4133C4D4C52}
            if (crc_id.length() < 42)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_guid = crc_id.substring(5, 41);
            UUID accountId = UUID.fromString(str_guid);
            if (!databaseProvider.getDataRole().removeAccount(accountId))
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return "OK";
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_del_facilitator(String payload, AtomicBoolean err)
    {
        try
        {
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.MC_ROOT_KEY);
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF{9C0A620B-3400-4F5D-87DD-D4133C4D4C52}
            if (crc_id.length() < 42)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_guid = crc_id.substring(5, 41);
            UUID facilitatorId = UUID.fromString(str_guid);
            if (!databaseProvider.getDataRole().removeFacilitator(facilitatorId))
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return "OK";
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_facilitator(String payload, AtomicBoolean err)
    {
        try
        {
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.MC_ROOT_KEY);
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF12345678
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_time = crc_id.substring(4);
            long timestamp_utc = Long.parseLong(str_time) * 1000; // in msec

            if ((System.currentTimeMillis() - timestamp_utc) > Utils.CHALLENGE_TIMEOUT)
            {
                err.set(true); return "WRNG5[" + (System.currentTimeMillis() - timestamp_utc) + "]" + "SERVER TIMESTAMP[" + System.currentTimeMillis() + "]CLIENT TIMESTAMP[" + timestamp_utc + "]";
            }

            String ret = databaseProvider.getDataRole().getFacilitator();
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_account(String str_facilitatorId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_facilitatorId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_facilitatorId + "]";
            }
            UUID facilitatorId = UUID.fromString(str_facilitatorId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getFacilitatorNetworkKey(facilitatorId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF12345678
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_time = crc_id.substring(4);
            long timestamp_utc = Long.parseLong(str_time) * 1000; // in msec

            if ((System.currentTimeMillis() - timestamp_utc) > Utils.CHALLENGE_TIMEOUT)
            {
                err.set(true); return "WRNG5[" + (System.currentTimeMillis() - timestamp_utc) + "]" + "SERVER TIMESTAMP[" + System.currentTimeMillis() + "]CLIENT TIMESTAMP[" + timestamp_utc + "]";
            }

            String ret = databaseProvider.getDataRole().getAccount(facilitatorId);
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_gateway(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF12345678
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_time = crc_id.substring(4);
            long timestamp_utc = Long.parseLong(str_time) * 1000; // in msec

            if ((System.currentTimeMillis() - timestamp_utc) > Utils.CHALLENGE_TIMEOUT)
            {
                err.set(true); return "WRNG5[" + (System.currentTimeMillis() - timestamp_utc) + "]" + "SERVER TIMESTAMP[" + System.currentTimeMillis() + "]CLIENT TIMESTAMP[" + timestamp_utc + "]";
            }

            String ret = databaseProvider.getDataRole().getGateway(accountId);
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_node(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF12345678
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_time = crc_id.substring(4);
            long timestamp_utc = Long.parseLong(str_time) * 1000; // in msec

            if ((System.currentTimeMillis() - timestamp_utc) > Utils.CHALLENGE_TIMEOUT)
            {
                err.set(true); return "WRNG5[" + (System.currentTimeMillis() - timestamp_utc) + "]" + "SERVER TIMESTAMP[" + System.currentTimeMillis() + "]CLIENT TIMESTAMP[" + timestamp_utc + "]";
            }

            String ret = databaseProvider.getDataRole().getNode(accountId);
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_gateway_details(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //012345678901234567890123456789012345
            //FFFF5136
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_id = crc_id.substring(4);

            String ret = databaseProvider.getDataRole().getGatewayDetails(accountId, Long.parseLong(str_id));
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;

        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_node_details(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //012345678901234567890123456789012345
            //FFFF5136
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_id = crc_id.substring(4);

            String ret = databaseProvider.getDataRole().getNodeDetails(accountId, Long.parseLong(str_id));
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;

        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_account_details(String str_facilitatorId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_facilitatorId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_facilitatorId + "]";
            }
            UUID facilitatorId = UUID.fromString(str_facilitatorId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getFacilitatorNetworkKey(facilitatorId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF{9C0A620B-3400-4F5D-87DD-D4133C4D4C52}
            if (crc_id.length() < 42)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_guid = crc_id.substring(5, 41);
            UUID accountId = UUID.fromString(str_guid);

            String ret = databaseProvider.getDataRole().getAccountDetails(facilitatorId, accountId);
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;

        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_get_facilitator_details(String payload, AtomicBoolean err)
    {
        try
        {
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.MC_ROOT_KEY);
            //          1         2         3         4
            //012345678901234567890123456789012345678901
            //FFFF{9C0A620B-3400-4F5D-87DD-D4133C4D4C52}
            if (crc_id.length() < 42)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }

            String str_guid = crc_id.substring(5, 41);
            UUID facilitatorId = UUID.fromString(str_guid);

            String ret = databaseProvider.getDataRole().getFacilitatorDetails(facilitatorId);
            if (ret == null)
            {
                err.set(true); return "WRNG6[NOT FOUND]";
            }
            return ret;
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_node_data(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //012345678901234567890123456789012345
            //FFFF5136
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_id = crc_id.substring(4);

            //check node ownership
            if (!databaseProvider.getDataRole().matchNodeAccount(accountId, Long.parseLong(str_id)))
            {
                err.set(true); return "WRNG5[OWNERSHIP]";
            }

            ArrayList<String> labels = new ArrayList<String>();
            ArrayList<String> rssis = new ArrayList<String>();
            ArrayList<String> lats = new ArrayList<String>();
            ArrayList<String> lons = new ArrayList<String>();

            StringBuilder sb = new StringBuilder();

            lock.readLock().lock();
            try
            {
                StoreTupleIoT storeTuple = databaseProvider.getStoreData().getStore().get(new Long(Long.parseLong(str_id)));
                if (storeTuple != null)
                {

                    ArrayList<StoreElement> eq_data = new ArrayList<StoreElement>();
                    long last_updated = 0;
                    int last_seq = 0;

                    for (int i = 0; i < storeTuple.getLast().get().size(); i++)
                    {
                        StoreElement l = storeTuple.getLast().get().get(i);

                        if (last_updated == 0)
                        {
                            last_updated = l.getTimestamp();
                        }
                        // test if the same data
                        ByteBuffer chk = ByteBuffer.wrap(l.getData());
                        chk.order(Processor.ORDER);
                        chk.getShort();  // rssi
                        chk.getInt(); // gateway_id
                        chk.get(); // crc4
                        int seq = chk.get() & 0xFF;

                        boolean last_eq = false;
                        if ((last_seq == 0) || (last_seq == seq))
                        {
                            last_seq = seq;
                            eq_data.add(l);

                            if (i != (storeTuple.getLast().get().size() - 1))
                            {
                                continue;
                            }

                            last_eq = true;
                        }

                        for (; ; )
                        {
                            byte[] data = getDataFromGroup_iot(eq_data);
                            long timestamp = getTimestampFromGroup_iot(eq_data);
                            double[] pos = getPosFromGroup_iot(eq_data);

                            eq_data.clear();
                            eq_data.add(l);
                            last_seq = seq;

                            labels.add(new Long(timestamp).toString());

                            if (pos != null)
                            {
                                lats.add(Utils.df6.get().format(pos[0]));
                                lons.add(Utils.df6.get().format(pos[1]));
                            }

                            ByteBuffer pckt = ByteBuffer.wrap(data);
                            pckt.order(Processor.ORDER);

                            int raw_rssi = pckt.getShort();
                            int rssi = ((raw_rssi - Utils.MIN_RSSI) * 100) / (Utils.MIN_RSSI * (-1));
                            if (rssi < 0)
                            {
                                rssi = 0;
                            }
                            else if (rssi > 100)
                            {
                                rssi = 100;
                            }

                            rssis.add(new Integer(rssi).toString());

                            if ((i == (storeTuple.getLast().get().size() - 1)) && (!last_eq))
                            {
                                // last one need processing too only in case it is different from the group
                                // if from the same group it is already processed
                                // <this part is here because we have already collected the last one and need to process it also>
                                last_eq = true;   // and make sure do not enter again in endless look
                                continue;
                            }

                            break;
                        }
                    }
                }
            }
            finally
            {
                lock.readLock().unlock();
            }

            sb.append("var deviceId = ").append(new Long(Long.parseLong(str_id))).append(";\n");
            sb.append("var deviceDayLabels = [");
            for (int i = 0; i < labels.size(); i++)
            {
                sb.append(labels.get(i));
                if (i < (labels.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");
            sb.append("var deviceDayRssi = [");
            for (int i = 0; i < rssis.size(); i++)
            {
                sb.append(rssis.get(i));
                if (i < (rssis.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");
            sb.append("var deviceDayLat = [");
            for (int i = 0; i < lats.size(); i++)
            {
                sb.append(lats.get(i));
                if (i < (lats.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");
            sb.append("var deviceDayLon = [");
            for (int i = 0; i < lons.size(); i++)
            {
                sb.append(lons.get(i));
                if (i < (lons.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");

            return sb.toString();


        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    String get_mc_gateway_data(String str_accountId, String payload, AtomicBoolean err)
    {
        try
        {
            if (str_accountId.length() < 38)
            {
                err.set(true); return "WRNG1[" + str_accountId + "]";
            }
            UUID accountId = UUID.fromString(str_accountId.substring(1, 37));
            byte[] array_key = databaseProvider.getDataRole().getAccountNetworkKey(accountId);
            if (array_key == null)
            {
                err.set(true); return "WRNG2[CANNOT GET KEY]";
            }
            String crc_id = XXTEAString.decryptBase64StringToString(payload, Utils.toHex(array_key));
            //          1         2         3
            //012345678901234567890123456789012345
            //FFFF5136
            if (crc_id.length() < 5)
            {
                err.set(true); return "WRNG3[SIGN]";
            }
            String crc = crc_id.substring(0, 4);
            if (crc.compareTo(XXTEACRCString.crc(crc_id.substring(4))) != 0)
            {
                err.set(true); return "WRNG4[SIGN]";
            }
            String str_id = crc_id.substring(4);
            //check gateway ownership
            if (!databaseProvider.getDataRole().matchGatewayAccount(accountId, Long.parseLong(str_id)))
            {
                err.set(true); return "WRNG5[OWNERSHIP]";
            }

            ArrayList<String> labels = new ArrayList<String>();
            ArrayList<String> histLabels = new ArrayList<String>();

            ArrayList<String> dataMoisture = new ArrayList<String>();
            ArrayList<String> dataTemp = new ArrayList<String>();

            ArrayList<String> histMoisture = new ArrayList<String>();
            ArrayList<String> histTemp = new ArrayList<String>();

            lock.readLock().lock();
            try
            {
                StoreTupleIoT storeTupple = databaseProvider.getStoreData().getGateways().get(new Long(Long.parseLong(str_id)));

                if (storeTupple != null)
                {
                    for (int i = 0; i < storeTupple.getLast().get().size(); i++)
                    {
                        StoreElement l = storeTupple.getLast().get().get(i);

                        ByteBuffer bb = ByteBuffer.wrap(l.getData());
                        bb.order(Processor.ORDER);
                        PacketDataWrapperGateway packetDataWrapperNode = new PacketDataWrapperGateway(bb);

                        labels.add(new Long(l.getTimestamp()).toString());


                        dataTemp.add(packetDataWrapperNode.getDegree(false));
                        dataMoisture.add(packetDataWrapperNode.getMoisture_percent());

                    }
                }


                if (storeTupple != null)
                {

                    for (int i = 0; i < storeTupple.getHist().get().size(); i++)
                    {
                        StoreDayIoT h = storeTupple.getHist().get().get(i);

                        StoreElement avg = Utils.getHistAVG(h);
                        if (avg == null)
                        {
                            continue;
                        }

                        ByteBuffer bb = ByteBuffer.wrap(avg.getData());
                        bb.order(Processor.ORDER);
                        PacketDataWrapperGateway packetDataWrapper = new PacketDataWrapperGateway(bb);

                        ////////////////////////////////////////////////////////////////////////////////////////////////////////////

                        histLabels.add(new Long(h.getLastTimestampForThisDay()).toString());
                        histTemp.add(packetDataWrapper.getDegree(false));
                        histMoisture.add(packetDataWrapper.getMoisture_percent());

                    }
                }

            }
            finally
            {
                lock.readLock().unlock();
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // build regular
            StringBuilder sb = new StringBuilder("var gatewayId = ");
            StringBuilder sb_site = new StringBuilder();
            sb_site.append(Long.parseLong(str_id));

            sb.append("\"").append(sb_site).append("\";\n");
            sb.append("var gatewayDayLabels = [");
            for (int i = 0; i < labels.size(); i++)
            {
                sb.append(labels.get(i));
                if (i < (labels.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");
            sb.append("var gatewayDayHumidity = [");
            for (int i = 0; i < dataMoisture.size(); i++)
            {
                sb.append(dataMoisture.get(i));
                if (i < (dataMoisture.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");

            sb.append("var gatewayDayTemp = [");
            for (int i = 0; i < dataTemp.size(); i++)
            {
                sb.append(dataTemp.get(i));
                if (i < (dataTemp.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");

            sb.append("var gatewayHistLabels = [");
            for (int i = 0; i < histLabels.size(); i++)
            {
                sb.append(histLabels.get(i));
                if (i < (histLabels.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");
            sb.append("var gatewayHistHumidity = [");
            for (int i = 0; i < histMoisture.size(); i++)
            {
                sb.append(histMoisture.get(i));
                if (i < (histMoisture.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");

            sb.append("var gatewayHistTemp = [");
            for (int i = 0; i < histTemp.size(); i++)
            {
                sb.append(histTemp.get(i));
                if (i < (histTemp.size() - 1))
                {
                    sb.append(',');
                }
            }
            sb.append("];\n");


            return sb.toString();


        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            err.set(true); return "WRNG7[" + th.getMessage() + "]";
        }
    }

    void disconnect(PacketChannel pc)
    {
        gatewayBuffer.removeGatewayChannel(pc);
    }

    void connect(long gateway_id, PacketChannel pc)
    {
        gatewayBuffer.addGatewayChannel(gateway_id, pc);
    }
}



