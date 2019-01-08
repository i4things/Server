package net.b2net.utils.iot.server;

import java.util.HashSet;
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
public class AccountData extends RoleData
{
    private final UUID id;
    private final UUID facilitatorId;

    private final HashSet<Long> nodes = new HashSet<Long>();
    private final HashSet<Long> gateways = new HashSet<Long>();

    public AccountData(UUID id,
                       UUID facilitatorId,
                       byte[] networkKey,
                       String name)
    {
        super(networkKey, name);
        this.id = id;
        this.facilitatorId = facilitatorId;
    }

    public final UUID getId()
    {
        return id;
    }

    public final UUID getFacilitatorId()
    {
        return facilitatorId;
    }

    public final HashSet<Long> getNodes()
    {
        return nodes;
    }

    public final HashSet<Long> getGateways()
    {
        return gateways;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof AccountData)
        {
            return ((AccountData) o).id.equals(id);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
}
