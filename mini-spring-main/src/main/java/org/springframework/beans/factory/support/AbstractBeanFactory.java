package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringValueResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author derekyi
 * @date 2020/11/22
 */
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory {

	private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

	private final Map<String, Object> factoryBeanObjectCache = new HashMap<>();

	private final List<StringValueResolver> embeddedValueResolvers = new ArrayList<StringValueResolver>();

	private ConversionService conversionService;


	//通过bean名寻找bean实例，如果不存在那就创建bean实例并返回
	@Override
	public Object getBean(String name) throws BeansException {
		//如果是已经存在于Singleton的三级缓存里的单例bean，那么找到并返回------场景:A引用B,B引用A,在createBean(A)、B未实例化时，会递归的createBean(B),
		Object sharedInstance = getSingleton(name);
		if (sharedInstance != null) {
			//如果是FactoryBean，从FactoryBean#getObject中创建bean实例
			return getObjectForBeanInstance(sharedInstance, name);  //这种直接返回getObject()也不做缓存，怎么感觉像是多例模式？
		}
		//否则
		BeanDefinition beanDefinition = getBeanDefinition(name);
		//创建bean实例，并会自动执行2钟postProcessor!
		Object bean = createBean(name, beanDefinition);
		return getObjectForBeanInstance(bean, name); //----这儿可以看出，实现FactoryBean<T>接口的代理方式的优先级是高于实现beanPostProcessor的！会将其覆盖~~~
	}

	/**
	 * 如果是FactoryBean，从FactoryBean#getObject中创建bean---MyBatis就是用的这种代理方法
	 *
	 * @param beanInstance
	 * @param beanName
	 * @return
	 */
	protected Object getObjectForBeanInstance(Object beanInstance, String beanName) {
		Object object = beanInstance;
		//------如果是factoryBean，那么就是实例化前返回代理对象~~~ ---FactoryBean表示这个Bean是一个Factory类型的Bean，即这是一个工厂
		if (beanInstance instanceof FactoryBean) {
			FactoryBean factoryBean = (FactoryBean) beanInstance;
			try {
				if (factoryBean.isSingleton()) {
					//singleton作用域bean，从缓存中获取----singleton的bean都会被存到factoryBeanObjectCache缓存里?
					object = this.factoryBeanObjectCache.get(beanName);
					if (object == null) {
						object = factoryBean.getObject();
						this.factoryBeanObjectCache.put(beanName, object); //存入缓存
					}
				} else {
					//prototype作用域bean
					object = factoryBean.getObject();
				}
			} catch (Exception ex) {
				throw new BeansException("FactoryBean threw exception on object[" + beanName + "] creation", ex);
			}
		}

		return object;
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return ((T) getBean(name));
	}

	@Override
	public boolean containsBean(String name) {
		return containsBeanDefinition(name);
	}

	protected abstract boolean containsBeanDefinition(String beanName);

	protected abstract Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException;

	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		//有则覆盖
		this.beanPostProcessors.remove(beanPostProcessor);
		this.beanPostProcessors.add(beanPostProcessor);
	}

	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		this.embeddedValueResolvers.add(valueResolver);
	}

	//---------在bean实例化后设置属性前执行，用于解析@value注解的属性值
	public String resolveEmbeddedValue(String value) {
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			result = resolver.resolveStringValue(result);
		}
		return result;
	}

	@Override
	public ConversionService getConversionService() {
		return conversionService;
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}
}
