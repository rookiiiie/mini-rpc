package github.javaguide.remoting.transport.netty.client;

import github.javaguide.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * unprocessed requests by the server.
 *
 * @author shuang.kou
 * @createTime 2020年06月04日 17:30:00
 */
public class ClientUnprocessedRequests {
    //客户端请求服务，生成一个future，当服务端执行完服务返回给客户端后，在客户端的readChannelHandler中调用complete(rpcResponse)唤醒请求线程，并传递rpcResponse。
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        //删除key，并返回value，若不存在key则返回null
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            //会唤醒客户端的rpcResponse = future.get()
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
