package github.javaguide.proxy;

import github.javaguide.compress.Compress;
import github.javaguide.config.RpcServiceConfig;
import github.javaguide.enums.RequestIdGenerateEnum;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.enums.RpcResponseCodeEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.factory.SingletonFactory;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;
import github.javaguide.remoting.fault.TolerantStrategy;
import github.javaguide.remoting.transport.RpcRequestTransport;
import github.javaguide.remoting.transport.netty.client.ClientUnprocessedRequests;
import github.javaguide.remoting.transport.netty.client.NettyRpcClient;
import github.javaguide.remoting.transport.socket.SocketRpcClient;
import github.javaguide.requestIdGenerate.RequestIdGenerate;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dynamic proxy class.
 * When a dynamic proxy object calls a method, it actually calls the following invoke method.
 * It is precisely because of the dynamic proxy that the remote method called by the client is like calling the local method (the intermediate process is shielded)
 *
 * @author shuang.kou
 * @createTime 2020年05月10日 19:01:00
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {

    private static final String INTERFACE_NAME = "interfaceName";

    /**
     * Used to send requests to the server.And there are two implementations: socket and netty
     */
    private final RpcRequestTransport rpcRequestTransport;
    private final RpcServiceConfig rpcServiceConfig;

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }


    public RpcClientProxy(RpcRequestTransport rpcRequestTransport) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = new RpcServiceConfig();
    }

    /**
     * get the proxy object
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    /**
     * This method is actually called when you use a proxy object to call a method.
     * The proxy object is the object you get through the getProxy method.
     * 代理的是@RpcReference注解的类的方法
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        log.info("invoked method: [{}]", method.getName());
        //在client端调用该远程service时，通过反射获取该method以及类的信息，封装成rpcRequest往zk中寻找可用的远程服务的channel，后续rpcRequest会被封装成rpcMessage与server交互
        RpcRequest rpcRequest = RpcRequest.builder().methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(ExtensionLoader.getExtensionLoader(RequestIdGenerate.class).getExtension(RequestIdGenerateEnum.SNOWFLAKE.getName()).getRequestId())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion())
//                .timeout(rpcServiceConfig.getTimeout())
//                .maxAttemptCnt(rpcServiceConfig.getMaxAttemptCnt())
                .build();
        RpcResponse<Object> rpcResponse = null;
        if (rpcRequestTransport instanceof NettyRpcClient) {
            //根据client容错机制指定调用策略，支持failover（失败转移）、failFast（快速失败）两种策略
            TolerantStrategy tolerantStrategy = SingletonFactory.getInstance(rpcServiceConfig.getFaultClass());
            rpcResponse = tolerantStrategy.doTolerant(rpcRequest,rpcServiceConfig);
//            rpcResponse = (RpcResponse<Object>) rpcRequestTransport.sendRpcRequest(rpcRequest);

            // 会阻塞，会被以下三种情况唤醒：
//            CompletableFuture 被正常完成（通过 complete(T value) 方法）。
//            CompletableFuture 被异常完成（通过 completeExceptionally(Throwable ex) 方法）。
//            当前线程被中断。
//            if(rpcRequest.getTimeout()!=-1){
//                try{
//                    rpcResponse = completableFuture.get(rpcRequest.getTimeout(), TimeUnit.SECONDS);
//                }catch (InterruptedException | ExecutionException e){
//                    completableFuture.cancel(true); // 取消任务
//                    log.info("服务调用超时失败，" + INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
//                    throw e;
//                }
//            }else{
//                rpcResponse = completableFuture.get();
//            }
        }
//        if (rpcRequestTransport instanceof SocketRpcClient) {
//            rpcResponse = (RpcResponse<Object>) rpcRequestTransport.sendRpcRequest(rpcRequest);
//        }

        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();
    }

    //
    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
