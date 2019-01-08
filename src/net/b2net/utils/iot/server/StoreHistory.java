package net.b2net.utils.iot.server;

import java.io.Serializable;
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
class StoreHistory implements Serializable
{
    private final static int MAX_HISTORY = 2 * 31;
    // one value per day
    // newest one in front
    ArrayList<StoreElement> history = new ArrayList<StoreElement>();

    void add(StoreElement e)
    {
        for (; (history.size() >= MAX_HISTORY); )
        {
            history.remove(history.size() - 1);
        }

        history.add(0, e);
    }

    ArrayList<StoreElement> get()
    {
        return history;
    }

}
