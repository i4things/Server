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

import java.io.Serializable;
import java.util.HashMap;

public class StoreDataIoT implements Serializable
{
    private HashMap<Long, StoreTupleIoT> store = new HashMap<Long, StoreTupleIoT>();
    private HashMap<Long, StoreTupleIoT> gateways = new HashMap<Long, StoreTupleIoT>();

    HashMap<Long, StoreTupleIoT> getStore()
    {
        return store;
    }

    HashMap<Long, StoreTupleIoT> getGateways()
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
