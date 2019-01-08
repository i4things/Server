package net.b2net.utils.iot.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
public class StoreDataRole implements Serializable
{
    public HashMap<UUID, FacilitatorData> allFacilitators = new HashMap<UUID, FacilitatorData>();
    public HashMap<UUID, AccountData> allAccounts = new HashMap<UUID, AccountData>();
    public HashMap<Long, GatewayData> allGateways = new HashMap<Long, GatewayData>();
    public HashMap<Long, NodeData> allNodes = new HashMap<Long, NodeData>();

    public Set<Long> freeGatewayIds = new HashSet<Long>();
    public Set<Long> freeNodeIds = new HashSet<Long>();

    public long maxGatewayId = 4100;
    public long maxNodeId = 1;

}
