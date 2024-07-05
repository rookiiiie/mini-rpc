package github.javaguide.remoting.fault;

import com.github.houbb.sisyphus.core.core.RetryWaiter;
import com.github.houbb.sisyphus.core.core.Retryer;
import com.github.houbb.sisyphus.core.support.condition.RetryConditions;
import github.javaguide.config.RpcServiceConfig;
import github.javaguide.enums.CompressTypeEnum;
import github.javaguide.enums.SerializationTypeEnum;
import github.javaguide.enums.ServiceDiscoveryEnum;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.factory.SingletonFactory;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.remoting.constants.RpcConstants;
import github.javaguide.remoting.dto.RpcMessage;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;
import github.javaguide.remoting.transport.netty.client.ChannelProvider;
import github.javaguide.remoting.transport.netty.client.ClientUnprocessedRequests;
import github.javaguide.remoting.transport.netty.client.NettyRpcClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 功能描述
 *
 * @author: gusang
 * @date: 2024年07月05日 15:05
 * failover策略：失败自动恢复策略，当调用失败时，通过再次负载均衡的方式切换至其他可用节点
 */
@Slf4j
public class FailFastTolerantStrategy implements TolerantStrategy{

    private final NettyRpcClient nettyRpcClient;
    private final ServiceDiscovery serviceDiscovery;
    private final ClientUnprocessedRequests clientUnprocessedRequests;
    private final ChannelProvider channelProvider;

    public FailFastTolerantStrategy() {
        this.clientUnprocessedRequests = SingletonFactory.getInstance(ClientUnprocessedRequests.class);
        this.nettyRpcClient = SingletonFactory.getInstance(NettyRpcClient.class);
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * 容错
     *
     * @param rpcRequest
     * @param rpcServiceConfig：包含重试次数、间隔时间等配置
     * @return
     */
    @Override
    public RpcResponse doTolerant(RpcRequest rpcRequest, RpcServiceConfig rpcServiceConfig) {
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // 将这个future放到unprocessed request中，用于再次接受消息后唤醒请求体
        clientUnprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);

        //---------增加在timeout内重试maxAttemptCnt次直到成功或者最终失败-----------
        RpcResponse<Object> rpcResponse = Retryer.<RpcResponse<Object>>newInstance()
                .maxAttempt(1) //最大尝试次数:快速失败策略下，只发起一次调用，失败立即抛出异常
                .condition(RetryConditions.hasExceptionCause())
//                .retryWaitContext(RetryWaiter.<RpcResponse<Object>>retryWait(NoRetryWait.class).context()) //无等待时间
                .retryWaitContext(RetryWaiter.<RpcResponse<Object>>retryWait(rpcServiceConfig.getTimeoutStrategy()).value(rpcServiceConfig.getTimeout()).context())
                .callable(new Callable<RpcResponse<Object>>() {
                    @Override
                    public RpcResponse<Object> call() throws Exception {
                        System.out.println("----------------------FailOverTolerantStrategy：尝试一次--------------------------------");
                        //剩余重试次数
                        // build return value
                        // get server address（负载均衡查找到提供指定服务的服务器） ----对于容错策略：若服务器失败，则查询一个新的
                        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
                        // get  server address related channel
                        Channel channel = nettyRpcClient.getChannel(inetSocketAddress);
                        if (channel.isActive()) {
                            //---------------rpcRequest->rpcMessage,因为rpc两端是通过rpcMessage格式的消息传播的------------------
                            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                                    .compress(CompressTypeEnum.GZIP.getCode())
                                    .messageType(RpcConstants.REQUEST_TYPE).build();

                            //ChannelFuture 对象表示了这个操作的异步结果
                            ChannelFuture channelFuture = channel.writeAndFlush(rpcMessage);
                            //阻塞channelFuture，直到改操作完成
                            channelFuture.awaitUninterruptibly();
                            if (channelFuture.isSuccess()) {
                                log.info("client send message: [{}]", rpcMessage);
                            }else{
                                log.error("Send failed:", channelFuture.cause());
                                throw new RuntimeException("rpc message send fail",channelFuture.cause());
                            }
                        } else {
                            throw new IllegalStateException();
                        }
                        return resultFuture.get();
                    }
                }).retryCall();
        return rpcResponse;
    }
}
