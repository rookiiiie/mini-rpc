package github.javaguide.spring;

import cn.hutool.core.util.StrUtil;
import github.javaguide.annotation.RpcService;
import github.javaguide.config.RpcServiceConfig;
import github.javaguide.enums.RpcRequestTransportEnum;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.factory.SingletonFactory;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.provider.impl.ZkServiceProviderImpl;
import github.javaguide.remoting.transport.RpcRequestTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

/**
 * 功能描述:实现一个扫描@RpcService bean的自定义spring 扫描器
 * 通过实现BeanFactoryPostProcessor接口，在所有BeanDefinition加载完成后，但在bean实例化之前，注册rpcService到IOC与zk中去
 *
 * 对于由实现BeanFactoryPostProcessor来创建的bean，由于已经存在于三级缓存，所以不会对该bean执行BeanPostProcessor接口的2个方法，详情见refresh()的finishBeanFactoryInitialization(beanFactory)方法
 *
 * @author: gusang
 * @date: 2024年06月23日 18:55
 */
@Slf4j
@Component
public class customScanner2 implements BeanFactoryPostProcessor {

    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    //指定要扫描的基础包
    private final String basePackage = "github.javaguide";

    public customScanner2() {
        //服务提供者（zookeeper）
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        //spi（Service Provider Interface）？
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension(RpcRequestTransportEnum.NETTY.getName());
    }

    /**
     * 在所有BeanDefinition加载完成后，但在bean实例化之前，提供修改BeanDefinition属性值的机制
     *
     * @param beanFactory
     * @throws BeansException
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider();
//        TypeFilter typeFilter = new AnnotationTypeFilter(RpcService.class);
//        scanner.addIncludeFilter(typeFilter);


        //scanner.findCandidateComponents：扫描基础包下被@RpcService注释的类，即rpc服务类，并返回set<BeanDefinition>
        scanner.findCandidateComponents(basePackage,RpcService.class).forEach(beanDefinition -> {
//            System.out.println("beanDefinition = "+beanDefinition.getBeanClass());
            try {
                //1.实例化并且添加该rpcService bean到IOC中
                Object bean = beanDefinition.getBeanClass().getDeclaredConstructor().newInstance();
                beanFactory.addSingleton(StrUtil.lowerFirst(beanDefinition.getBeanClass().getSimpleName()),bean); //使用无参构造一个实例并加入ioc容器
                //2.将该service注册到zk中
                if (bean.getClass().isAnnotationPresent(RpcService.class)) {
                    System.out.println("注册服务：" + bean.getClass());
                    log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
                    // get RpcService annotation
                    RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
                    // build RpcServiceProperties
                    RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                            .group(rpcService.group())
                            .version(rpcService.version())
                            .service(bean).build();
                    serviceProvider.publishService(rpcServiceConfig);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
