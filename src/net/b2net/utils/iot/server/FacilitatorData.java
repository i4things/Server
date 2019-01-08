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
public class FacilitatorData extends RoleData
{
    private final UUID id;

    private final HashSet<UUID> accounts = new HashSet<UUID>();

    public FacilitatorData(UUID id,
                           byte[] networkKey,
                           String name)
    {
        super(networkKey, name);
        this.id = id;
    }

    public final UUID getId()
    {
        return id;
    }


    public final HashSet<UUID> getAccounts()
    {
        return accounts;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof FacilitatorData)
        {
            return ((FacilitatorData) o).id.equals(id);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

}
