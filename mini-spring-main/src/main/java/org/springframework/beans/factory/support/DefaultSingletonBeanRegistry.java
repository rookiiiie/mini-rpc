package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;

/**
 * @author derekyi
 * @date 2020/11/22
 */
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {

	//一级缓存
	private Map<String, Object> singletonObjects = new HashMap<>();

	//二级缓存
	private Map<String, Object> earlySingletonObjects = new HashMap<>();

	//三级缓存
	private Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>();

	//存储用于destroy bean的bean~~~
	private final Map<String, DisposableBean> disposableBeans = new HashMap<>();


	@Override
	public Object getSingleton(String beanName) {
		Object singletonObject = singletonObjects.get(beanName);
		if (singletonObject == null) {
			singletonObject = earlySingletonObjects.get(beanName);
			if (singletonObject == null) {
				ObjectFactory<?> singletonFactory = singletonFactories.get(beanName);
				if (singletonFactory != null) {
					//当为bean注入某个外部依赖bean时，若这个外部依赖bean未初始化完成（即仅实例化，外部依赖字段未赋值），则会被放到第三级缓存中（见doCreateBean()）提前暴露，则此时即能取到该未初始化完成的外部依赖bean~
					singletonObject = singletonFactory.getObject();
					//从三级缓存放进二级缓存（对于代理对象，会先被放到第三级缓存，当再次被调用的话会被放到）
					earlySingletonObjects.put(beanName, singletonObject);
					singletonFactories.remove(beanName);
				}
			}
		}
		return singletonObject;
	}

	@Override
	public void addSingleton(String beanName, Object singletonObject) {
		singletonObjects.put(beanName, singletonObject);
		earlySingletonObjects.remove(beanName);
		singletonFactories.remove(beanName);
	}

	//如果允许循环依赖的话，Spring 就会将刚刚实例化完成，但是属性还没有初始化完的 Bean 对象给提前暴露出去，这里通过 addSingletonFactory 方法，向三级缓存中添加一个 ObjectFactory 对象
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		singletonFactories.put(beanName, singletonFactory);
	}

	public void registerDisposableBean(String beanName, DisposableBean bean) {
		disposableBeans.put(beanName, bean);
	}

	public void destroySingletons() {
		ArrayList<String> beanNames = new ArrayList<>(disposableBeans.keySet());
		for (String beanName : beanNames) {
			DisposableBean disposableBean = disposableBeans.remove(beanName);
			try {
				disposableBean.destroy();
			} catch (Exception e) {
				throw new BeansException("Destroy method on bean with name '" + beanName + "' threw an exception", e);
			}
		}
	}
}
