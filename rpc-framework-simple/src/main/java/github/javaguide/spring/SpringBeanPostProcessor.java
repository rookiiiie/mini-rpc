package github.javaguide.spring;

import github.javaguide.annotation.RpcReference;
import github.javaguide.annotation.RpcService;
import github.javaguide.config.RpcServiceConfig;
import github.javaguide.enums.RpcRequestTransportEnum;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.factory.SingletonFactory;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.provider.impl.ZkServiceProviderImpl;
import github.javaguide.proxy.RpcClientProxy;
import github.javaguide.remoting.transport.RpcRequestTransport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * call this method before creating the bean to see if the class is annotated
 * 1.实例化：创建Bean实例。
 * 2.依赖注入：根据Spring配置文件或注解注入依赖（包括字段注入和构造函数注入）。
 * 3.初始化：调用初始化回调方法，如afterPropertiesSet和@PostConstruct。
 * BeanPostProcessor的postProcessBeforeInitialization：在初始化回调之前执行自定义逻辑。
 * BeanPostProcessor的postProcessAfterInitialization：在所有初始化工作完成之后执行自定义逻辑。
 *
 * @author shuang.kou
 * @createTime 2020年07月14日 16:42:00
 */
@Slf4j
@Component
//首先通过@Component加入spring的beanFactory，然后在refresh的invokeBeanFactoryPostProcessors(beanFactory);中通过beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);找到该bean并执行2个重载方法
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient; //支持netty、socket

    public SpringBeanPostProcessor() {
        //服务提供者（zookeeper）
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        //spi（Service Provider Interface）？
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension(RpcRequestTransportEnum.NETTY.getName());
    }

    //BeanPostProcessor是spring提供的扩展点：Bean的初始化之前执行
    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
//        System.out.println("=============postProcessBeforeInitialization，beanName = " + beanName);
//        //对于server的@RpcService 服务，不能懒加载！！！ 否则client不能在zk中找到该服务，会报错！
//        //被@RpcService注解注释的类提供rpc服务，将服务注册到zookeeper中
//        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
//            System.out.println("注册服务：" + beanName);
//            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
//            // get RpcService annotation
//            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
//            // build RpcServiceProperties
//            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
//                    .group(rpcService.group())
//                    .version(rpcService.version())
//                    .service(bean).build();
//            serviceProvider.publishService(rpcServiceConfig);
//        }
        return bean;
    }

    //BeanPostProcessor是spring提供的扩展点：Bean的初始化后执行，这里是主动注入远程服务的代理对象
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            //为被@RpcReference注解注释的字段【即远程服务引用实例】创建代理对象（掩盖网络调用的细节）
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group()) //指定组
                        .version(rpcReference.version()) //指定版本
                        .timeout(rpcReference.timeout())//指定超时时间
                        .maxAttemptCnt(rpcReference.maxAttemptCnt())//指定最大重试次数
                        .faultClass(rpcReference.faultClass()) //容错策略
                        .timeoutStrategy(rpcReference.timeoutStrategy()) //超时策略
                        .build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());
                // 设置字段可访问
                declaredField.setAccessible(true);
                try {
                    // 将代理实例赋值给目标Bean的对应字段
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
        return bean;
    }
}
