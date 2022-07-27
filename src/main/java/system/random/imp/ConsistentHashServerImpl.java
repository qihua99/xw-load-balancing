package system.random.imp;

import org.apache.log4j.Logger;
import system.common.ConnectUtil;
import system.common.GetHashCode;
import system.entity.Server;
import system.random.BalanceService;

import java.util.*;

/**
 * 一致性Hash实现类
 *
 * @author xuwei
 * @date 2022/07/18 10:41
 **/
public class ConsistentHashServerImpl implements BalanceService {
    /**
     * 虚拟节点数
     */
    private final Integer vnnNodeCount;
    /**
     * 一致性hash环
     */
    private final TreeMap<Integer, Server> treeMapHash;

    private static final Logger logger = Logger.getLogger(ConsistentHashServerImpl.class);


    public ConsistentHashServerImpl(List<Server> serverList, Integer vnnNodeCount) {
        this.vnnNodeCount = vnnNodeCount;
        TreeMap<Integer, Server> treeMapHash = new TreeMap<>();
        for (Server server : serverList) {
            int hash = GetHashCode.getHashCode(server.getAddress() + server.getPort());
            treeMapHash.put(hash, server);
            for (int i = 1; i <= this.vnnNodeCount; i++) {
                treeMapHash.put(GetHashCode.getHashCode(server.getAddress() + server.getPort() + "&&" + i), server);
            }
        }
        this.treeMapHash = treeMapHash;
    }

    /**
     * 获取服务器
     *
     * @param requestNumber  请求量
     * @param requestAddress 请求地址
     * @return
     */
    @Override
    public Server getServer(int requestNumber, String requestAddress) {
        Server server;
        synchronized (treeMapHash) {
            if (treeMapHash.isEmpty()) {
                logger.warn("Don not have server available!");
                return null;
            }
            int hash = GetHashCode.getHashCode(requestAddress);
            // 向右寻找第一个 key
            Map.Entry<Integer, Server> subEntry = treeMapHash.ceilingEntry(hash);
            // 设置成一个环，如果超过尾部，则取第一个点
            subEntry = subEntry == null ? treeMapHash.firstEntry() : subEntry;
            server = subEntry.getValue();
        }
        return server;
    }

    /**
     * 添加服务器节点
     *
     * @param server server
     */
    @Override
    public void addServerNode(Server server) {
        synchronized (treeMapHash) {
            int hash = GetHashCode.getHashCode(server.getAddress());
            treeMapHash.put(hash, server);
            for (int i = 1; i <= vnnNodeCount; i++) {
                int vnnNodeHash = GetHashCode.getHashCode(server.getAddress() + server.getPort() + "&&" + i);
                treeMapHash.put(vnnNodeHash, server);
            }
        }
    }

    /**
     * 删除服务器节点
     *
     * @param server server
     */
    @Override
    public void delServerNode(Server server) {
        synchronized (treeMapHash) {
            int hash = GetHashCode.getHashCode(server.getAddress() + server.getPort());
            treeMapHash.remove(hash);
            for (int i = 1; i <= vnnNodeCount; i++) {
                int vnnNodeHash = GetHashCode.getHashCode(server.getAddress() + server.getPort() + "&&" + i);
                treeMapHash.remove(vnnNodeHash);
            }
        }
    }


}
