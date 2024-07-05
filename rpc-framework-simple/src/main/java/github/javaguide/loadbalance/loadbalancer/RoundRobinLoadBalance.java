package github.javaguide.loadbalance.loadbalancer;

import github.javaguide.loadbalance.AbstractLoadBalance;
import github.javaguide.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于轮询的负载均衡（通过线程安全的索引实现即可）
 *
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {
    private final AtomicLong indexHolder = new AtomicLong(0);
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        long index = indexHolder.getAndIncrement();
        if(indexHolder.get()<0){
            indexHolder.set(0);
        }
        int actualIndex =  (int)(index % (long)serviceAddresses.size());
        return serviceAddresses.get(actualIndex);
    }
}
