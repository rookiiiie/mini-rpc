package github.javaguide;

import github.javaguide.annotation.RpcReference;
import github.javaguide.remoting.fault.FailFastTolerantStrategy;
import github.javaguide.remoting.fault.FailOverTolerantStrategy;
import org.springframework.stereotype.Component;

/**
 * @author smile2coder
 */
@Component
public class HelloController {

    //在BeanPostProcessor的postProcessAfterInitialization中通过生成代理类来注入远程服务
    @RpcReference(version = "version1", group = "test1", timeout = 5, maxAttemptCnt = 3,faultClass = FailOverTolerantStrategy.class)
    private HelloService helloService;

    public void test() throws InterruptedException {
        String hello = this.helloService.hello(new Hello("111", "222"));
        //如需使用 assert 断言，需要在 VM options 添加参数：-ea
        assert "Hello description is 222".equals(hello);
        Thread.sleep(12000);
        for (int i = 0; i < 10; i++) {
            System.out.println(helloService.hello(new Hello("111", "222")));
        }
    }
}
