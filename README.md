# mini-rpc
一个rpc框架，支持多种功能，并可灵活扩展

### 本项目参考：

1.guide哥的rpc框架（https://github.com/Snailclimb/guide-rpc-framework）

2.DerekYRC佬的mini-spring框架（https://github.com/DerekYRC/mini-spring）

3.马哥的Retryer框架（https://github.com/houbb/sisyphus）

  


### 学习这些项目的同时，扩展了额外的功能，如下：

+ 由于mini-spring不支持扫描指定注解，因此简单修改了mini-spring，并且修改了原guide-rpc服务端注册@RpcService到ioc、zk以及客户端为@Reference字段注入代理对象的时机

+ 通过SPI配置生成请求ID的方式，除了原有的UUID外，增加了改良版SeataSnowflake方式（改进优点见下）

+ 增加了多种容错策略（failover、failfast），通过@Reference注解指定，并可以灵活扩展

+ 整合sisyphus-Retryer框架，支持多种超时重试策略（如固定间隔、指数增加间隔等），并可以灵活扩展

+ 支持客户端本地缓存，采用定时+延迟获取混合的方式更新缓存

+ 增加了轮询的负载均衡方式

### 接下来逐步介绍上述优化的代码以及原因

##### 优化1:
 ***1.1*** 由于mini-spring不支持扫描指定注解到IOC，为了支持这个功能，本项目修改了package org.springframework.context.annotation包下的
 ``` 
	public Set<BeanDefinition> findCandidateComponents(String basePackage,Class T) {
		Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
		// 扫描有Class的类（如org.springframework.stereotype.Component注解）
		Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation(basePackage, T);
		for (Class<?> clazz : classes) {
			BeanDefinition beanDefinition = new BeanDefinition(clazz);
			candidates.add(beanDefinition);
		}
		return candidates;
	}
 ```
 
 ***1.2*** rpc的服务的注册与发现，顺序肯定是先注册服务，再发现服务，结合mini-spring而言，本项目分别在refresh流程的先后2个扩展点先后执行服务的注册与发现。

 首先我们先看一下mini-spring的**refresh()流程**（该方法位于org.springframework.context.support）
 
 ```
public void refresh() throws BeansException {
		//创建BeanFactory，并加载BeanDefinition
		refreshBeanFactory();
		//这个beanFactory里面有Configurable用途的bean！比如各种Processor！！！
		ConfigurableListableBeanFactory beanFactory = getBeanFactory(); //这个地方的getBeanFactory()得到的就是上面一行创建的beanFactory~~~

		//Aware接口：可以感知context，ApplicationContextAwareProcessor实现了BeanPostProcessor，实现了 ApplicationContextAware接口的Bean可以获取到ApplicationContext（即 Spring 容器本身）
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this)); //beanPost:增加的是bean实例化后的处逻辑

		//扩展点1：在beanFactory装载好所有BeanDefinition之后，执行BeanFactoryPostProcessor(利用beanFactory.getBeansOfType(BeanFactoryPostProcessor.class)获取该类Processor）
		invokeBeanFactoryPostProcessors(beanFactory); //注:这个方法先于下面的postProcessBeforeInitialization()！！！

		//BeanPostProcessor需要提前与其他bean实例化之前注册-----提前注册，当实例化完后再调用~~~
		registerBeanPostProcessors(beanFactory); //这玩意在AbstractAutowireCapableBeanFactory-initializeBean()里会执行，即BeanPostProcessors中的两个方法:postProcessBeforeInitialization()和postProcessAfterInitialization()方法

		//初始化事件发布者---管理事件、事件监听器
		initApplicationEventMulticaster();

		//注册事件监听器----注册事件
		registerListeners();

		//注册类型转换器和提前实例化单例bean---？？？类型转化器干啥用的？（见扩展篇）首先实例化单例bean吗:是的，先实例化单例且不为lazyLoad的bean~~
		//扩展点2：实例化中会涉及很多processor（包括在初始化后的代理processor~）
		finishBeanFactoryInitialization(beanFactory);

		//发布容器刷新完成事件---？干啥用的。。？
		finishRefresh();
}
 ```
 ##### refresh()的流程可归纳如下：
 > 1.扫包映射成BeanDefinition 

 > 2.执行扩展点1: BeanFactoryPostProcessor(服务端注册@RpcService服务到IOC与zk)

 > 3.调用构造器实例化Bean 

 > 4.为bean外部依赖注入IOC中的依赖

 > 5.执行扩展点2: BeanPostProcessor (为客户端@Reference字段注入代理对象)

从上述流程中，可以很清晰的看出，服务的注册与发现分别是在先后的两个扩展点实现的！

当你实现了扩展点接口的方法后，mini-spring会在指定的时机从IOC中取实现了改扩展点接口的方法的bean，并执行改扩展方法，如扩展点1:BeanFactoryPostProcessor接口

我们Ctrl进到执行扩展点1的方法
```
invokeBeanFactoryPostProcessors(beanFactory); 
```
可以看到，spring是通过beanFactory.getBeansOfType(BeanFactoryPostProcessor.class)来从IOC容器中获取实现了这个扩展点接口的bean，并拿出来依次执行改扩展点方法~
```
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		//获取BeanFactoryPostProcessor类即用于postProcessor的bean!~~~（如：BeanFactoryPostProcessor[这里便是]和BeanPostProcessor）
		Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);
		for (BeanFactoryPostProcessor beanFactoryPostProcessor : beanFactoryPostProcessorMap.values()) {
			beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
		}
	}
```
本项目的第一个扩展点执行@RpcService服务的注册，位于如下路径
![image](https://github.com/user-attachments/assets/e2255fe6-6167-4e1e-a127-8946af86dbcc)
第二个扩展点执行@Reference字段的代理对象注入，位于如下路径
![image-1](https://github.com/user-attachments/assets/84bb3d9c-92aa-4a90-88d9-6b7889299f22)
##### 优化2:
原本的guide-rpc中使用了UUID作为请求体ID，由于UUID其**空间消耗大(128bit)、不安全(基于MAC生成、非递增)**等缺点，本项目增加了改良版Seata-Snowflake算法，用于生成分布式全局唯一的请求ID，可通过SPI的方式来灵活配置。

该算法代码路径如下：
![image-2](https://github.com/user-attachments/assets/384d6e34-6d5f-44e0-bb36-40e7a486ae33)

 ###### 原版Snowflake的结构如下：
 > 64bit(1b sign + timestamp 41b  + 10b workerId + 12b sequence)
 
 ID依赖于当前时间，一台机器在一个时间戳单位(1ms)下可以生成2^12 个唯一ID，QPS约为400W，但该算法存在以下两个问题:

 **1.时间回拨问题:** 当机器时间倒退回之前某一毫秒，而该毫秒内已经生成了2^12个ID,则会导致生成的ID重复
 
 **2.生成不稳定问题** 1ms内生成了超过2^12个ID，则会不够用

###### 为了解决上述两个问题，有了改良版的Seata-Snowflake，结构如下：

> 64bit(1b sign + 10b workerId + **41b timestamp + 12b sequence**)

该改良算法从结构上调换了workerId与timestamp的顺序，这样一来，**(41b timestamp + 12b sequence)便是一个整体，timestamp为高位，sequence为低位**。该算法只有在初始时取当前时间为timestamp，后续生成的过程中**不再依赖当前时间**。

当在1个时间戳单位(1ms)内生成2^12 +1个ID时，当生成到2^12+1个ID时，此时sequence已经满了，则作为高位的timestamp进1，低位sequence从0开始计数（即为常规的整数加法进位的过程）。

由于Seata-Snowflake在生成ID的过程不依赖时间，因此不存在时间回拨问题，同时也不存在生成不稳定问题，很好的解决了上述两个问题。

##### 优化3:
当客户端rpc服务调用rpc服务时，由于网络或一些原因导致该rpc服务不可用，为了应对这种情况，本项目在客户端提供了**单个服务级别**的容错服务，支持**failover（故障转移）、failfast（快速失败）策略**。可在@RpcReference引用中配置，如下：

![image-4](https://github.com/user-attachments/assets/5acbbe5b-5dcc-478e-9abb-177b27c8808b)

当然，也可以通过实现如下TolerantStrategy接口的doTolerant方法自定义你想要的容错逻辑。
![image-3](https://github.com/user-attachments/assets/1a14c09c-dfed-46c8-8470-13bf26d7b6a2)

##### 优化4:
整合sisyphus-Retryer框架，支持多种超时重试策略（如固定间隔、指数增加间隔等），可在@RpcReference引用中配置，如下：
![image-6](https://github.com/user-attachments/assets/c11d88da-fa23-4878-9c27-497f7c1341f5)

当然，你也可以像固定间隔时间一样，通过继承com.github.houbb.sisyphus.core.support.wait.AbstractRetryWait或实现RetryWait接口来灵活定义自己想要的重试策略~

为什么不使用常用的spring-Retry、guava-Retry框架？

###### 不使用spring-Retry的原因

spring-Retry只支持通过**捕捉Throwable的子类**来控制重试逻辑（即只能基于异常来控制重试），不支持通过数据对象本身来控制重试逻辑。

###### 不使用guava-Retry的原因

guava支持通过数据对象来控制重试逻辑，但是它不支持注解的方式

sisphus则是对guava-Retry的封装，提供了注解的方式，具体配置、使用内容见[sisyphus官网](https://github.com/houbb/sisyphus)

##### 优化5:
场景1：当客户端频繁重复调用一个rpc服务时，需要重复的通过网络请求向注册中心获取服务提供者的地址，开销很大

场景2：当客户端频繁重复调用一个rpc服务时，注册中心不可用，但实际的服务提供者是可用的

为了解决上述场景的问题，决定引**入本地缓存**，并采用**定时+延迟**获取混合的方式更新缓存。

由于这儿对缓存的使用需求比较简单，因此只采用了线程安全的hashmap存储服务地址，代码见github.javaguide.registry.zk.ZkServiceDiscoveryImpl类

本地缓存的作用：
> 1.减少网络请求
> 2.减少对注册中心的依赖

