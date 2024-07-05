package github.javaguide.remoting.fault;

import github.javaguide.config.RpcServiceConfig;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;

public interface TolerantStrategy {
    /**
     * 容错
     * @return
     */
    RpcResponse doTolerant(RpcRequest rpcRequest, RpcServiceConfig rpcServiceConfig);
}
