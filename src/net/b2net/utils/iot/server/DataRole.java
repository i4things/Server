package net.b2net.utils.iot.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;

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
public class DataRole
{
    private final int MIN_FREE_GATEWAY_LIST_SIZE = 2000;
    private final int MIN_FREE_NODE_LIST_SIZE = 2000;

    private final StoreDataRole store;

    private final ReadWriteLock lock;

    public DataRole(ReadWriteLock lock,
                    StoreDataRole store)
    {
        this.store = store;
        this.lock = lock;
    }


    public final UUID getAccount(long nodeId)
    {
        lock.readLock().lock();
        try
        {
            NodeData n = store.allNodes.get(nodeId);
            if (n == null)
            {
                return null;
            }

            return n.getAccountId();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final byte[] getFacilitatorNetworkKey(UUID id)
    {
        lock.readLock().lock();
        try
        {
            FacilitatorData f = store.allFacilitators.get(id);
            if (f == null)
            {
                return null;
            }

            return f.getNetworkKey();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final byte[] getAccountNetworkKey(UUID id)
    {
        lock.readLock().lock();
        try
        {
            AccountData a = store.allAccounts.get(id);
            if (a == null)
            {
                return null;
            }

            return a.getNetworkKey();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final boolean isNodeValid(long nodeId)
    {
        lock.readLock().lock();
        try
        {
            return store.allNodes.containsKey(nodeId);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final boolean matchGatewayNode(long gatewayId,
                                          long nodeId)
    {
        lock.readLock().lock();
        try
        {
            GatewayData g = store.allGateways.get(gatewayId);
            if (g == null)
            {
                return false;
            }

            NodeData n = store.allNodes.get(nodeId);
            if (n == null)
            {
                return false;
            }


            return g.getAccountId().equals(n.getAccountId());
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public boolean matchNodeAccount(UUID id,
                                    long nodeId)
    {
        lock.readLock().lock();
        try
        {

            NodeData n = store.allNodes.get(nodeId);
            if (n == null)
            {
                return false;
            }


            return n.getAccountId().equals(id);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public boolean matchGatewayAccount(UUID id,
                                       long gatewayId)
    {
        lock.readLock().lock();
        try
        {
            GatewayData g = store.allGateways.get(gatewayId);
            if (g == null)
            {
                return false;
            }

            return g.getAccountId().equals(id);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final byte[] getGatewayNetworkKey(long id)
    {
        lock.readLock().lock();
        try
        {
            GatewayData g = store.allGateways.get(id);
            if (g == null)
            {
                return null;
            }

            return g.getNetworkKey();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final double[] getGatewayPos(long id)
    {
        lock.readLock().lock();
        try
        {
            GatewayData g = store.allGateways.get(id);
            if (g == null)
            {
                return null;
            }

            return new double[]{g.getLat(), g.getLon()};
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final byte[] getNodeNetworkKey(long id)
    {
        lock.readLock().lock();
        try
        {
            NodeData n = store.allNodes.get(id);
            if (n == null)
            {
                return null;
            }

            return n.getNetworkKey();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    public final boolean addFacilitator(UUID id, byte[] networkKey, String name)
    {
        lock.writeLock().lock();
        try
        {
            if (store.allFacilitators.containsKey(id))
            {
                return false;
            }

            FacilitatorData f = new FacilitatorData(id, networkKey, name);

            store.allFacilitators.put(id, f);

            return true;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public final boolean addAccount(UUID id, UUID facilitatorId, byte[] networkKey, String name)
    {
        lock.writeLock().lock();
        try
        {
            FacilitatorData f = store.allFacilitators.get(facilitatorId);
            if (f == null)
            {
                return false;
            }

            if (store.allAccounts.containsKey(id))
            {
                return false;
            }

            AccountData a = new AccountData(id, facilitatorId, networkKey, name);

            f.getAccounts().add(id);
            store.allAccounts.put(id, a);

            return true;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public final Long addGateway(UUID accountId, byte[] networkKey, double lat, double lon, boolean open, String name)
    {
        lock.writeLock().lock();
        try
        {
            AccountData a = store.allAccounts.get(accountId);
            if (a == null)
            {
                return null;
            }

            Long free_gi = null;
            if (store.freeGatewayIds.size() >= MIN_FREE_GATEWAY_LIST_SIZE)
            {
                for (Long gi : store.freeGatewayIds)
                {
                    if (open)
                    {
                        if ((gi & 1) == 0)
                        {
                            store.freeGatewayIds.remove(gi);
                            free_gi = gi;
                            break;
                        }
                    }
                    else
                    {
                        store.freeGatewayIds.remove(gi);
                        free_gi = gi;
                        break;
                    }
                }
            }

            if (free_gi == null)
            {
                free_gi = store.maxGatewayId;
                store.maxGatewayId++;

                if (open)
                {
                    if ((free_gi & 1) != 0)
                    {
                        // get next one and put this in free list as we need even number
                        store.freeGatewayIds.add(free_gi);
                        free_gi = store.maxGatewayId;
                        store.maxGatewayId++;
                    }
                }
            }

            GatewayData g = new GatewayData(free_gi, accountId, networkKey, lat, lon, name);

            a.getGateways().add(free_gi);
            store.allGateways.put(free_gi, g);

            return free_gi;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public Long addNode(UUID accountId, byte[] networkKey, String name)
    {
        lock.writeLock().lock();
        try
        {
            AccountData a = store.allAccounts.get(accountId);
            if (a == null)
            {
                return null;
            }

            Long free_ni = null;
            if (store.freeNodeIds.size() >= MIN_FREE_NODE_LIST_SIZE)
            {
                for (Long ni : store.freeNodeIds)
                {
                    store.freeNodeIds.remove(ni);
                    free_ni = ni;
                    break;
                }
            }

            if (free_ni == null)
            {
                free_ni = store.maxNodeId;
                store.maxNodeId++;
            }

            NodeData n = new NodeData(free_ni, accountId, networkKey, name);

            a.getNodes().add(free_ni);
            store.allNodes.put(free_ni, n);

            return free_ni;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public final boolean removeNode(long id)
    {
        lock.writeLock().lock();
        try
        {
            NodeData n = store.allNodes.remove(id);
            if (n == null)
            {
                return false;
            }

            AccountData na = store.allAccounts.get(n.getAccountId());
            if (na == null)
            {
                return false;
            }

            na.getNodes().remove(id);
            store.freeNodeIds.add(id);

            return true;

        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public final boolean removeGateway(long id)
    {
        lock.writeLock().lock();
        try
        {
            GatewayData g = store.allGateways.remove(id);
            if (g == null)
            {
                return false;
            }

            AccountData ga = store.allAccounts.get(g.getAccountId());
            if (ga == null)
            {
                return false;
            }

            ga.getGateways().remove(id);
            store.freeGatewayIds.add(id);

            return true;

        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public final boolean removeAccount(UUID id)
    {
        lock.writeLock().lock();
        try
        {
            AccountData a = store.allAccounts.remove(id);
            if (a == null)
            {
                return false;
            }

            FacilitatorData fa = store.allFacilitators.get(a.getFacilitatorId());
            if (fa == null)
            {
                return false;
            }

            fa.getAccounts().remove(id);

            for (Long g : a.getGateways())
            {
                store.allGateways.remove(g);
                store.freeGatewayIds.add(g);
            }

            for (Long n : a.getNodes())
            {
                store.allNodes.remove(n);
                store.freeNodeIds.add(n);
            }

            a.getNodes().clear();
            a.getGateways().clear();

            return true;

        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public final boolean removeFacilitator(UUID id)
    {
        lock.writeLock().lock();
        try
        {
            FacilitatorData f = store.allFacilitators.remove(id);
            if (f == null)
            {
                return false;
            }

            for (UUID a : f.getAccounts())
            {
                removeAccount(a);
            }

            f.getAccounts().clear();

            return true;

        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public final String getAccount(UUID facilitatorId)
    {
        HashSet<UUID> copy;
        lock.readLock().lock();
        try
        {
            FacilitatorData f = store.allFacilitators.get(facilitatorId);
            if (f == null)
            {
                return null;
            }

            copy = (HashSet<UUID>) f.getAccounts().clone();
        }
        finally
        {
            lock.readLock().unlock();
        }

        StringBuilder sb = new StringBuilder();
        for (UUID account : copy)
        {
            AccountData a = null;
            lock.readLock().lock();
            try
            {
                a = store.allAccounts.get(account);
            }
            finally
            {
                lock.readLock().unlock();
            }
            sb.append("{").append(a.getId().toString().toUpperCase()).append("},\"").append(a.getName().replaceAll("\"", "\\\"")).append("\";");
        }
        if (sb.length() > 0)
        {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public final String getAccountDetails(UUID facilitatorId, UUID accountId)
    {
        AccountData a = null;
        lock.readLock().lock();
        try
        {
            FacilitatorData f = store.allFacilitators.get(facilitatorId);
            if (f == null)
            {
                return null;
            }

            if (f.getAccounts().contains(accountId))
            {
                a = store.allAccounts.get(accountId);
            }
        }
        finally
        {
            lock.readLock().unlock();
        }

        if (a != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{").append(a.getId().toString().toUpperCase()).append("},\"").append(a.getName().replaceAll("\"", "\\\"")).append("\",").append(Utils.toHex(a.getNetworkKey()));

            return sb.toString();
        }
        else
        {
            return null;
        }
    }

    public final String getFacilitatorDetails(UUID facilitatorId)
    {
        FacilitatorData f = null;
        lock.readLock().lock();
        try
        {
            f = store.allFacilitators.get(facilitatorId);
        }
        finally
        {
            lock.readLock().unlock();
        }

        if (f != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append('{').append(f.getId().toString().toUpperCase()).append("},\"").append(f.getName().replaceAll("\"", "\\\"")).append("\",").append(Utils.toHex(f.getNetworkKey()));

            return sb.toString();
        }
        else
        {
            return null;
        }
    }

    public final String getFacilitator()
    {
        HashMap<UUID, FacilitatorData> copy;
        lock.readLock().lock();
        try
        {
            copy = (HashMap<UUID, FacilitatorData>) store.allFacilitators.clone();
        }
        finally
        {
            lock.readLock().unlock();
        }

        StringBuilder sb = new StringBuilder();
        for (FacilitatorData facilitator : copy.values())
        {
            sb.append('{').append(facilitator.getId().toString().toUpperCase()).append("},\"").append(facilitator.getName().replaceAll("\"", "\\\"")).append("\";");
        }
        if (sb.length() > 0)
        {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public final String getGatewayDetails(UUID accountId, Long gatewayId)
    {
        GatewayData g = null;
        lock.readLock().lock();
        try
        {
            AccountData a = store.allAccounts.get(accountId);
            if (a == null)
            {
                return null;
            }

            if (a.getGateways().contains(gatewayId))
            {
                g = store.allGateways.get(gatewayId);
            }
        }
        finally
        {
            lock.readLock().unlock();
        }

        if (g != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(g.getId()).append(",\"").append(g.getName().replaceAll("\"", "\\\"")).append("\",").append(Utils.toHex(g.getNetworkKey())).append(',').append(g.getLat()).append(',').append(g.getLon()).append(',').append(((g.getId() & 1) == 0) ? "TRUE" : "FALSE");

            return sb.toString();
        }
        else
        {
            return null;
        }
    }

    public final String getGateway(UUID accountId)
    {
        HashSet<Long> copy;
        StringBuilder sb = new StringBuilder();
        lock.readLock().lock();
        try
        {
            AccountData a = store.allAccounts.get(accountId);
            if (a == null)
            {
                return null;
            }

            copy = (HashSet<Long>) a.getGateways().clone();

            for (Long gateway : copy)
            {
                GatewayData g = null;

                g = store.allGateways.get(gateway);

                sb.append(g.getId()).append(",\"").append(g.getName().replaceAll("\"", "\\\"")).append("\";");
            }
        }
        finally
        {
            lock.readLock().unlock();
        }
        if (sb.length() > 0)
        {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public final String getNodeDetails(UUID accountId, Long nodeId)
    {
        NodeData n = null;
        lock.readLock().lock();
        try
        {
            AccountData a = store.allAccounts.get(accountId);
            if (a == null)
            {
                return null;
            }

            if (a.getNodes().contains(nodeId))
            {
                n = store.allNodes.get(nodeId);
            }
        }
        finally
        {
            lock.readLock().unlock();
        }

        if (n != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(n.getId()).append(",\"").append(n.getName().replaceAll("\"", "\\\"")).append("\",").append(Utils.toHex(n.getNetworkKey()));

            return sb.toString();
        }
        else
        {
            return null;
        }
    }

    public final String getNode(UUID accountId)
    {
        HashSet<Long> copy;
        StringBuilder sb = new StringBuilder();
        lock.readLock().lock();
        try
        {
            AccountData a = store.allAccounts.get(accountId);
            if (a == null)
            {
                return null;
            }

            copy = (HashSet<Long>) a.getNodes().clone();

            for (Long node : copy)
            {
                NodeData n = null;

                n = store.allNodes.get(node);

                sb.append(n.getId()).append(",\"").append(n.getName().replaceAll("\"", "\\\"")).append("\";");
            }

        }
        finally
        {
            lock.readLock().unlock();
        }

        if (sb.length() > 0)
        {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}

