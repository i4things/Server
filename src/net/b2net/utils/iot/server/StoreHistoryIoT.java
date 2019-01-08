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
import java.util.ArrayList;

public class StoreHistoryIoT implements Serializable
{
    // collection of days - similar to last of the entitiy above //StoreTuple/
    ArrayList<StoreDayIoT> history = new ArrayList<StoreDayIoT>();

    void add(StoreDayIoT e)
    {
        for (; (history.size() >= Utils.MAX_HISTORY); )
        {
            history.remove(history.size() - 1);
        }

        history.add(0, e);
    }

    ArrayList<StoreDayIoT> get()
    {
        return history;
    }
}
