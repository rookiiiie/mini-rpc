package github.javaguide.annotation;


import com.github.houbb.sisyphus.api.support.wait.RetryWait;
import com.github.houbb.sisyphus.core.support.wait.FixedRetryWait;
import github.javaguide.remoting.fault.FailOverTolerantStrategy;
import github.javaguide.remoting.fault.TolerantStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC reference annotation, autowire the service implementation class
 *
 * @author smile2coder
 * @createTime 2020年09月16日 21:42:00
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

    //容错策略，支持failover、failFast。只有failover时，maxRetries才有效，每次重试则会通过轮询策略替换同group内的其他备用服务端。
//    String faultToleranceStrategy() default "";

    //超时最大重试次数
    int maxAttemptCnt() default 1;
    //超时时间，仅在某些容错策略下有效
    int timeout() default 30;
    //超时策略，默认为固定间隔时间重试。可指定为sisphus-core-0.1.0.jar/com.github.houbb.sisyphus.core.support.wait下的任意一个策略，或者也可以实现RetryWait接口或继承AbstractRetryWait模板改实现自定义超时策略~
    Class<? extends RetryWait> timeoutStrategy() default com.github.houbb.sisyphus.core.support.wait.FixedRetryWait.class;
    //容错策略，默认为失败自动恢复
    Class<? extends TolerantStrategy> faultClass() default FailOverTolerantStrategy.class;

}
