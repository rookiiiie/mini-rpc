package github.javaguide.remoting.transport.netty.client;


import com.github.houbb.sisyphus.core.core.RetryWaiter;
import com.github.houbb.sisyphus.core.support.condition.RetryConditions;
import com.github.houbb.sisyphus.core.support.wait.FixedRetryWait;
import com.github.houbb.sisyphus.core.support.wait.NoRetryWait;
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
import github.javaguide.remoting.transport.RpcRequestTransport;
import github.javaguide.remoting.transport.netty.codec.RpcMessageDecoder;
import github.javaguide.remoting.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.github.houbb.sisyphus.core.core.Retryer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * initialize and close Bootstrap object
 *
 *
 * @author shuang.kou
 * @createTime 2020年05月29日 17:51:00
 */
@Slf4j
//将client调用的远程服务通过在spring的BeanPostProcessor中通过动态代理+注解 替代被注入的服务
public final class NettyRpcClient implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery;
    private final ClientUnprocessedRequests clientUnprocessedRequests;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;


    public NettyRpcClient() {
        // initialize resources such as EventLoopGroup, Bootstrap
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //  The timeout period of the connection.
                //  If this time is exceeded or the connection cannot be established, the connection fails.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // If no data is sent to the server within 15 seconds, a heartbeat request is sent
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder());
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(new NettyRpcClientHandler());
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
        this.clientUnprocessedRequests = SingletonFactory.getInstance(ClientUnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * connect server and get the channel ,so that you can send rpc message to server
     *
     * @param inetSocketAddress server address
     * @return the channel
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
//        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
//        // 将这个future放到unprocessed request中，用于再次接受消息后唤醒请求体
//        clientUnprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
//
//        //---------增加在timeout内重试maxAttemptCnt次直到成功或者最终失败-----------
//        RpcResponse<Object> rpcResponse = Retryer.<RpcResponse<Object>>newInstance()
//                .maxAttempt(rpcRequest.getMaxAttemptCnt()) //最大尝试次数
//                .condition(RetryConditions.hasExceptionCause())
////                .retryWaitContext(RetryWaiter.<RpcResponse<Object>>retryWait(NoRetryWait.class).context()) //无等待时间
//                .retryWaitContext(RetryWaiter.<RpcResponse<Object>>retryWait(FixedRetryWait.class).value(rpcRequest.getTimeout()).context())
//                .callable(new Callable<RpcResponse<Object>>() {
//                    @Override
//                    public RpcResponse<Object> call() throws Exception {
//                        System.out.println("----------------------尝试一次--------------------------------");
//                        //剩余重试次数
//                        // build return value
//                        // get server address（负载均衡查找到提供指定服务的服务器） ----对于容错策略：若服务器失败，则查询一个新的
//                        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
//                        // get  server address related channel
//                        Channel channel = getChannel(inetSocketAddress);
//                        if (channel.isActive()) {
//                            //---------------rpcRequest->rpcMessage,因为rpc两端是通过rpcMessage格式的消息传播的------------------
//                            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
//                                    .codec(SerializationTypeEnum.HESSIAN.getCode())
//                                    .compress(CompressTypeEnum.GZIP.getCode())
//                                    .messageType(RpcConstants.REQUEST_TYPE).build();
//
//                            //ChannelFuture 对象表示了这个操作的异步结果
//                            ChannelFuture channelFuture = channel.writeAndFlush(rpcMessage);
//                            //阻塞channelFuture，直到改操作完成
//                            channelFuture.awaitUninterruptibly();
//                            if (channelFuture.isSuccess()) {
//                                log.info("client send message: [{}]", rpcMessage);
//                            }else{
//                                log.error("Send failed:", channelFuture.cause());
//                                throw new RuntimeException("rpc message send fail",channelFuture.cause());
//                            }
//                        } else {
//                            throw new IllegalStateException();
//                        }
//                        return resultFuture.get();
//                    }
//                }).retryCall();

        //1.无响应即timeout且还有次数，则重新发送。若无次数则抛出异常 2.响应成功，则判断响应是否timeout，若时则1，若不是则返回response
        //重写时，模仿mini-mq，需要将writeAndFlush都封装在内，每次内部都要拿到响应才行（每次新建一个CompletableFuture），上面的玩意甚至都可以封装起来。
        //只要调用失败（无论是resultFuture.get超时还是！channel.isActive()还是返回状态码失败），则抛出RuntimeException异常，Retry框架会捕获并且重试的---其中resultFuture.get超时可以主动抛出异常来被Retry框架捕获

        //所以返回值直接返回response即可~~~

//        // build return value
//        resultFuture = new CompletableFuture<>()
//        // get server address（负载均衡查找到提供指定服务的服务器）
//        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
//        // get  server address related channel
//        Channel channel = getChannel(inetSocketAddress);
//        if (channel.isActive()) {
//            // 将这个future放到unprocessed request中，用于再次接受消息后唤醒请求体
//            clientUnprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
//            //---------------rpcRequest->rpcMessage,因为rpc两端是通过rpcMessage格式的消息传播的------------------
//            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
//                    .codec(SerializationTypeEnum.HESSIAN.getCode())
//                    .compress(CompressTypeEnum.GZIP.getCode())
//                    .messageType(RpcConstants.REQUEST_TYPE).build();
//
//
//            //ChannelFuture 对象表示了这个操作的异步结果
//            ChannelFuture channelFuture = channel.writeAndFlush(rpcMessage);
//            //该回调函数会在 Netty 的 I/O 线程中执行
//            channelFuture.addListener(new ChannelFutureListener() {
//                //发送成功或失败：由netty发布该类型的监听事件
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//                    //-------------------------------------------------------------------------
//                    if (future.isSuccess()) {
//                        log.info("client send message: [{}]", rpcMessage);
//                    } else {
//                        //以下4种情况会触发：
//                        //1.连接已经关闭或未建立成功
//                        //2.写入操作本身失败
//                        //3.发生异常，例如网络异常、编解码异常等
//                        //4.超时
//                        future.channel().close();
//                        resultFuture.completeExceptionally(future.cause()); //发生异常，唤醒future.get()
//                        log.error("Send failed:", future.cause());
//                    }
//                }
//            });
//        } else {
//            throw new IllegalStateException();
//        }

//        return rpcResponse;
        return null;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
