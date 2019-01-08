package net.b2net.utils.iot.server;

import java.io.Serializable;
import java.util.HashMap;

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
public class StoreData implements Serializable
{
    private HashMap<Short, HashMap<Short, StoreTuple>> store = new HashMap<Short, HashMap<Short, StoreTuple>>();
    private HashMap<Short, Long> gateways = new HashMap<Short, Long>();

    HashMap<Short, HashMap<Short, StoreTuple>> getStore()
    {
        return store;
    }

    HashMap<Short, Long> getGateways()
    {
        return gateways;
    }

//    @Override
//    public String toString() {
//        return "StoreData{" +
//                "store=" + store.size() +
//                ", gateways=" + gateways.size() +
//                '}';
//    }
}
