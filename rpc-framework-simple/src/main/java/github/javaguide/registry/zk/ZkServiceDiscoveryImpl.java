package github.javaguide.registry.zk;

import github.javaguide.enums.LoadBalanceEnum;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.loadbalance.LoadBalance;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.registry.zk.util.CuratorUtils;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.utils.CollectionUtil;
import github.javaguide.utils.MavenDependencyLoaderUtil;
import github.javaguide.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * service discovery based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年06月01日 15:16:00
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;
    private ConcurrentHashMap<String,List<String>> cache;
    //用于主动更新服务器本地缓存cache的线程
    ExecutorService executorService;
    private final static long interval = 5;
    public ZkServiceDiscoveryImpl() {
        //spi
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOADBALANCE.getName());
        this.cache = new ConcurrentHashMap<>();
//        executorService = Executors.newScheduledThreadPool(1);
//        //注册到线程池工厂，统一管理
//        ThreadPoolFactoryUtil.registerThreadPool("service-local-cache",executorService);
//        //定时主动更新
//        ((ScheduledExecutorService)executorService).scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                refreshServiceUrlList();
//            }
//        }, 0, interval, TimeUnit.SECONDS);
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        System.out.println("尝试获取服务："+rpcServiceName);
        List<String> serviceUrlList = cache.get(rpcServiceName);
        //首先从缓存中拿，若缓存没有，则惰性更新缓存该rpcServiceName对应的服务列表
        if(CollectionUtil.isEmpty(serviceUrlList)){
            CuratorFramework zkClient = CuratorUtils.getZkClient();
            serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
            if (CollectionUtil.isEmpty(serviceUrlList)) {
                throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
            }
            cache.putIfAbsent(rpcServiceName,serviceUrlList);
        }
//        System.out.println("serviceUrlList = " + serviceUrlList);
        // load balancing
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }

    public void refreshServiceUrlList(){
        log.info("主动refreshServiceUrlList");
        //更新cache
        List<String> serviceUrlList = null;
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        for(String rpcServiceName:cache.keySet()){
            serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
            log.info("定时拉取服务："+rpcServiceName);
            cache.put(rpcServiceName,serviceUrlList);
        }
    }
}
