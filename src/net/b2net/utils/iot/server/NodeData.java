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
public class NodeData extends RoleData
{
    private final long id;
    private final UUID accountId;

    public NodeData(long id,
                    UUID accountId,
                    byte[] networkKey,
                    String name)
    {
        super(networkKey, name);
        this.id = id;
        this.accountId = accountId;
    }

    public final long getId()
    {
        return id;
    }


    public final UUID getAccountId()
    {
        return accountId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof NodeData)
        {
            return (((NodeData) o).id == id);
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return new Long(id).hashCode();
    }

}
