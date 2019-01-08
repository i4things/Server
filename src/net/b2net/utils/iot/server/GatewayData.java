package net.b2net.utils.iot.server;

import java.util.UUID;

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
public class GatewayData extends NodeData
{
    private double lat;
    private double lon;

    public GatewayData(long id,
                       UUID accountId,
                       byte[] networkKey,
                       double lat,
                       double lon,
                       String name)
    {
        super(id, accountId, networkKey, name);
        this.lat = lat;
        this.lon = lon;
    }

    public final double getLat()
    {
        return lat;
    }

    public final double getLon()
    {
        return lon;
    }

    public final void setLat(double lat)
    {
        this.lat = lat;
    }

    public final void setLon(double lon)
    {
        this.lon = lon;
    }
}
