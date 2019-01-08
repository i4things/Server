package net.b2net.utils.iot.server;

import java.io.Serializable;
import java.util.ArrayList;

public class StoreDayIoT implements Serializable
{

    // oldest is last
    ArrayList<StoreElement> last = new ArrayList<StoreElement>();

    public ArrayList<StoreElement> get()
    {
        return last;
    }


    public final long getLastTimestampForThisDay()
    {
        if (last.size() > 0)
        {
            return last.get(0).getTimestamp();
        }
        return 0;
    }


}
