package github.javaguide.spring;

import github.javaguide.annotation.RpcScan;
import github.javaguide.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.context.ResourceLoaderAware;
//import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
//import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
//import org.springframework.core.type.AnnotationMetadata;
//import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.stereotype.Component;

/**
 * scan and filter specified annotations（by annotation @RpcScan)
 *
 * @author shuang.kou
 * @createTime 2020年08月10日 22:12:00
 */
//@Slf4j
public class CustomScannerRegistrar
//        implements ImportBeanDefinitionRegistrar, ResourceLoaderAware
{
//    private static final String SPRING_BEAN_BASE_PACKAGE = "github.javaguide";
//    private static final String BASE_PACKAGE_ATTRIBUTE_NAME = "basePackage";
//    private ResourceLoader resourceLoader;
//
//    @Override
//    public void setResourceLoader(ResourceLoader resourceLoader) {
//        this.resourceLoader = resourceLoader;
//
//    }
//
//    @Override
//    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
//        //get the attributes and values ​​of RpcScan annotation
//        //通过 annotationMetadata 获取到 @RpcScan 注解的属性和值
//        AnnotationAttributes rpcScanAnnotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcScan.class.getName()));
//        String[] rpcScanBasePackages = new String[0];
//        if (rpcScanAnnotationAttributes != null) {
//            // get the value of the basePackage property
//            rpcScanBasePackages = rpcScanAnnotationAttributes.getStringArray(BASE_PACKAGE_ATTRIBUTE_NAME);
//        }
//        if (rpcScanBasePackages.length == 0) {
//            rpcScanBasePackages = new String[]{((StandardAnnotationMetadata) annotationMetadata).getIntrospectedClass().getPackage().getName()};
//        }
//        //--------这里区分注册@Component与@RpcService的类的原因：对@RpcService类需要注册到zk中去----------
//        // Scan the RpcService annotation---扫描@RpcService注解
//        CustomScanner rpcServiceScanner = new CustomScanner(beanDefinitionRegistry, RpcService.class);
//        // Scan the Component annotation
//        CustomScanner springBeanScanner = new CustomScanner(beanDefinitionRegistry, Component.class);
//        if (resourceLoader != null) {
//            rpcServiceScanner.setResourceLoader(resourceLoader);
//            springBeanScanner.setResourceLoader(resourceLoader);
//        }
//        //---扫描SPRING_BEAN_BASE_PACKAGE路径下的@Component注解的类并注册到spring中
//        int springBeanAmount = springBeanScanner.scan(SPRING_BEAN_BASE_PACKAGE);
//        log.info("springBeanScanner扫描的数量 [{}]", springBeanAmount);
//        //---扫描rpcScanBasePackages路径下的@RpcService注解的类并注册到spring中
//        int rpcServiceCount = rpcServiceScanner.scan(rpcScanBasePackages);
//        log.info("rpcServiceScanner扫描的数量 [{}]", rpcServiceCount);
//
//    }

}
