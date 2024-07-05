package github.javaguide.config;

import com.github.houbb.sisyphus.api.support.wait.RetryWait;
import com.github.houbb.sisyphus.core.support.wait.FixedRetryWait;
import github.javaguide.remoting.fault.FailOverTolerantStrategy;
import github.javaguide.remoting.fault.TolerantStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author shuang.kou
 * @createTime 2020年07月21日 20:23:00
 *
 * 除了服务的组、版本外，还包含了该服务的容错策略、重试时间、重试次数、重试策略
 **/
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceConfig {
    /**
     * service version
     */
    private String version = "";
    /**
     * when the interface has multiple implementation classes, distinguish by group
     */
    private String group = "";
    private int timeout = 5;
    private int maxAttemptCnt = 1;
    private Class<? extends TolerantStrategy> faultClass = FailOverTolerantStrategy.class;
    Class<? extends RetryWait> timeoutStrategy = FixedRetryWait.class;
    /**
     * target service
     */
    private Object service;

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
