package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Collection;
import java.util.Map;

/**
 * 抽象应用上下文
 *
 * @author derekyi
 * @date 2020/11/28
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	public static final String CONVERSION_SERVICE_BEAN_NAME = "conversionService";

	private ApplicationEventMulticaster applicationEventMulticaster;

	//相当于Test里 创建BeanFactory、注册BeanDefiniton、自动实例化单例Bean、BeanFactoryPostProcessor、BeanPostProcessor的识别装载等
	//ApplicationContext就帮你把spring-bean里的东西的各个生命周期的步骤都组装起来成为一个流程
	@Override
	public void refresh() throws BeansException {
		//创建BeanFactory，并加载BeanDefinition
		refreshBeanFactory();
		//这个beanFactory里面有Configurable用途的bean！比如各种Processor！！！
		ConfigurableListableBeanFactory beanFactory = getBeanFactory(); //这个地方的getBeanFactory()得到的就是上面一行创建的beanFactory~~~

		//Aware接口：可以感知context，ApplicationContextAwareProcessor实现了BeanPostProcessor，实现了 ApplicationContextAware接口的Bean可以获取到ApplicationContext（即 Spring 容器本身）
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this)); //beanPost:增加的是bean实例化后的处逻辑

		//在beanFactory装载好所有BeanDefinition之后，执行BeanFactoryPostProcessor(利用beanFactory.getBeansOfType(BeanFactoryPostProcessor.class)获取该类Processor）
		invokeBeanFactoryPostProcessors(beanFactory); //注:这个方法先于下面的postProcessBeforeInitialization()！！！

		//BeanPostProcessor需要提前与其他bean实例化之前注册-----提前注册，当实例化完后再调用~~~
		registerBeanPostProcessors(beanFactory); //这玩意在AbstractAutowireCapableBeanFactory-initializeBean()里会执行，即BeanPostProcessors中的两个方法:postProcessBeforeInitialization()和postProcessAfterInitialization()方法

		//初始化事件发布者---管理事件、事件监听器
		initApplicationEventMulticaster();

		//注册事件监听器----注册事件
		registerListeners();

		//注册类型转换器和提前实例化单例bean---？？？类型转化器干啥用的？（见扩展篇）首先实例化单例bean吗:是的，先实例化单例且不为lazyLoad的bean~~
		//实例化中会涉及很多processor（包括在初始化后的代理processor~）
		finishBeanFactoryInitialization(beanFactory);

		//发布容器刷新完成事件---？干啥用的。。？
		finishRefresh();
	}

	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		//设置类型转换器
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME)) {
			Object conversionService = beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME);
			if (conversionService instanceof ConversionService) {
				beanFactory.setConversionService((ConversionService) conversionService);
			}
		}

		//提前实例化单例bean
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * 创建BeanFactory，并加载BeanDefinition ---模板模式，延迟到子类实现
	 *
	 * @throws BeansException
	 */
	protected abstract void refreshBeanFactory() throws BeansException;

	/**
	 * 在bean实例化之前，执行BeanFactoryPostProcessor
	 *
	 * @param beanFactory
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		//获取BeanFactoryPostProcessor类即用于postProcessor的bean!~~~(如：BeanFactoryPostProcessor[这里便是]和BeanPostProcessor）
		Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);
		for (BeanFactoryPostProcessor beanFactoryPostProcessor : beanFactoryPostProcessorMap.values()) {
			beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * 注册BeanPostProcessor：将实现了BeanPostProcessor接口的bean都注册到List<BeanPostProcessor>中去
	 *
	 * @param beanFactory
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		Map<String, BeanPostProcessor> beanPostProcessorMap = beanFactory.getBeansOfType(BeanPostProcessor.class);
		for (BeanPostProcessor beanPostProcessor : beanPostProcessorMap.values()) {
			beanFactory.addBeanPostProcessor(beanPostProcessor);
		}
	}

	/**
	 * 初始化事件发布者
	 */
	protected void initApplicationEventMulticaster() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
		beanFactory.addSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, applicationEventMulticaster);
	}

	/**
	 * 注册事件监听器：将实现了ApplicationListener接口的bean都注册到applicationEventMulticaster中去
	 */
	protected void registerListeners() {
		Collection<ApplicationListener> applicationListeners = getBeansOfType(ApplicationListener.class).values();
		for (ApplicationListener applicationListener : applicationListeners) {
			applicationEventMulticaster.addApplicationListener(applicationListener);
		}
	}

	/**
	 * 发布容器刷新完成事件
	 */
	protected void finishRefresh() {
		publishEvent(new ContextRefreshedEvent(this));
	}

	@Override
	public void publishEvent(ApplicationEvent event) {
		applicationEventMulticaster.multicastEvent(event);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return getBeanFactory().getBeansOfType(type);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public Object getBean(String name) throws BeansException {
		return getBeanFactory().getBean(name);
	}

	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	public abstract ConfigurableListableBeanFactory getBeanFactory();

	public void close() {
		doClose();
	}

	public void registerShutdownHook() {
		Thread shutdownHook = new Thread() {
			public void run() {
				doClose();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);

	}

	protected void doClose() {
		//---------------发布容器关闭事件，触发监听ContextClosedEvent类型事件的监听器--------
		publishEvent(new ContextClosedEvent(this));

		//执行单例bean的销毁方法
		destroyBeans();
	}

	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}
}

