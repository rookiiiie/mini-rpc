package github.javaguide.remoting.handler;

import github.javaguide.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * unprocessed requests by the server.
 *
 */
public class ServerUnprocessedRequests {
    //用于客户端重试同一请求时，服务器端保存该请求至map，并通过CompletableFuture判断是否执行完毕
    //如：客户端发送第一次请求（requestIDx），超过重试间隔时间后发送第二次请求（requestIDx），服务器端收到第二次请求后，该请求若存在于map，并且该future
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            //会唤醒客户端的rpcResponse = future.get()
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
