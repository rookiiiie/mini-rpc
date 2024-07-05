package org.springframework.beans.factory.support;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.*;
import org.springframework.core.convert.ConversionService;

import java.lang.reflect.Method;

/**
 * @author derekyi
 * @date 2020/11/22
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {

	private InstantiationStrategy instantiationStrategy = new SimpleInstantiationStrategy();

	@Override
	protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
		//-----------这是一个提前代理的时机（利用BeanPostProcessor接口）
		Object bean = resolveBeforeInstantiation(beanName, beanDefinition);
		if (bean != null) {
			return bean;
		}
//		System.out.println("bean("+beanName+") after resolveBeforeInstantiation");
		//------------------若bean需要被代理，则会在这个方法中（bean实例化后的processor中）被代理！【注意是bean已经被实例化后的时刻】-------------------
		return doCreateBean(beanName, beanDefinition);
	}

	/**
	 * 执行InstantiationAwareBeanPostProcessor的方法，如果bean需要代理，直接返回代理对象
	 *
	 * @param beanName
	 * @param beanDefinition
	 * @return
	 */
	protected Object resolveBeforeInstantiation(String beanName, BeanDefinition beanDefinition) {
		Object bean = applyBeanPostProcessorsBeforeInstantiation(beanDefinition.getBeanClass(), beanName);
		if (bean != null) {
			bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
		}
		return bean;
	}

	protected Object applyBeanPostProcessorsBeforeInstantiation(Class beanClass, String beanName) {
		for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
			//InstantiationAwareBeanPostProcessor接口类型的Processor表明需要对原bean进行代理
			if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
				Object result = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessBeforeInstantiation(beanClass, beanName);
				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}

	protected Object doCreateBean(String beanName, BeanDefinition beanDefinition) {
		Object bean;
		try {
//			System.out.println("create bean:" + beanName + ",beanDefinition = "+beanDefinition);
			//1.通过反射+cglib生成实例（未完全初始化，外部注入的字段还未初始化）
			bean = createBeanInstance(beanDefinition);

    //---------------------为解决循环依赖问题，将实例化后的bean放进缓存中提前暴露（这一步对于需要代理与不需要代理的bean都适用~~~）---------------------
	/**-------------------------------------------------------------------------------------------------------------------------------------------
	 * 对于在之后的initializeBean()里的beanPostProcessor里代理的bean，需要在这一步提前暴露好“还未被代理的bean”即所谓的装饰类：ObjectFactory<Object>“到第三级缓存中去，
	 * 此时真正的被装饰的代理bean还未被实例化[即只有调用ObjectFactory.getObject()时才会创建代理bean]！
	 * -------------------------------------------------------------------------------------------------------------------------------------------
	 */
			//2.对于未初始化完成的单例bean，会先将其存入第三级缓存（即提前暴露），
			// 当发生循环引用时（A里ref=B，B里ref=A），getBean(A)时，将A存入第三级缓存提前暴露，然后递归getBean(B)，赋值A时，从第三级缓存中读取到未完全初始化的A后并赋值，完成B的赋值
			if (beanDefinition.isSingleton()) {
				Object finalBean = bean;
				//在这里将未注入外部依赖的（未完全初始化的）bean提前放到第三级缓存中，提前暴露。
				addSingletonFactory(beanName, new ObjectFactory<Object>() {
					@Override
					public Object getObject() throws BeansException {
						//在还未完全初始化前-提前暴露-用于解决循环依赖----<<若这个bean是在实例化阶段代理的bean(即实现了BeanPostProcessor接口的bean），则会返回代理好的bean，将这个代理bean（而非原本的bean）添加到第三级单例缓存
						return getEarlyBeanReference(beanName, beanDefinition, finalBean);
					}
				});
			}
			//3.为未完全初始化的bean注入外部依赖 + 执行一些processor如
			boolean continueWithPropertyPopulation = applyBeanPostProcessorsAfterInstantiation(beanName, bean);
			if (!continueWithPropertyPopulation) {
				return bean;
			}
			//---------------在设置bean属性之前，允许BeanPostProcessor修改属性值-----------
			applyBeanPostProcessorsBeforeApplyingPropertyValues(beanName, bean, beanDefinition);
			//---------为bean填充属性，这一步可能会递归的createBean一个为实例化的引用---------<<<<由于引用依赖先于beanPostProcessor式的创建代理，所以需要上面的提前暴露当前bean实例到第三级缓存中去！
			applyPropertyValues(beanName, bean, beanDefinition);
			//执行bean的初始化方法和BeanPostProcessor的前置和后置处理方法------若bean需要被代理，则会在这一步中被代理！！！
			bean = initializeBean(beanName, bean, beanDefinition);
		} catch (Exception e) {
			throw new BeansException("Instantiation of bean failed", e);
		}

		//从beanDefinition里的destroyMethodName属性值注册有销毁方法的bean
		registerDisposableBeanIfNecessary(beanName, bean, beanDefinition);

		Object exposedObject = bean;
		if (beanDefinition.isSingleton()) {
			//如果有代理对象，此处获取代理对象
			exposedObject = getSingleton(beanName);

			//--------------------对于一个bean，在实例化、初始化(即已经注入好各种依赖之后)之后，会讲这个bean添加到一级单例缓存---------------------------
			addSingleton(beanName, exposedObject);
			//---------------------------------------------------------------------
		}
		return exposedObject;
	}

	//提前把原本应该再beanPostProcessor里的代理操作在getEarlyBeanReference里执行了，提前返回代理好的类，并且为了不再重复代理，会把这个代理好的类放到
	//当然了，对于不需要代理的类，直接返回原bean即可
	protected Object getEarlyBeanReference(String beanName, BeanDefinition beanDefinition, Object bean) {
		Object exposedObject = bean;
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof InstantiationAwareBeanPostProcessor) { //InstantiationAwareBeanPostProcessor为代理处理器接口~~~
				exposedObject = ((InstantiationAwareBeanPostProcessor) bp).getEarlyBeanReference(exposedObject, beanName); //---若被重载，则会返回代理对象
				if (exposedObject == null) {
					return exposedObject;
				}
			}
		}

		return exposedObject;
	}

	/**
	 * bean实例化后执行，如果返回false，不执行后续设置属性的逻辑
	 *
	 * @param beanName
	 * @param bean
	 * @return
	 */
	private boolean applyBeanPostProcessorsAfterInstantiation(String beanName, Object bean) {
		boolean continueWithPropertyPopulation = true;
		for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
			if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
				if (!((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessAfterInstantiation(bean, beanName)) {
					continueWithPropertyPopulation = false;
					break;
				}
			}
		}
		return continueWithPropertyPopulation;
	}

	/**
	 * 在设置bean属性之前，允许BeanPostProcessor修改属性值
	 *
	 * @param beanName
	 * @param bean
	 * @param beanDefinition
	 */
	protected void applyBeanPostProcessorsBeforeApplyingPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
		for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
			if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
				PropertyValues pvs = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessPropertyValues(beanDefinition.getPropertyValues(), bean, beanName);
				if (pvs != null) {
					for (PropertyValue propertyValue : pvs.getPropertyValues()) {
						beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
					}
				}
			}
		}
	}

	/**
	 * 注册有销毁方法的bean，即bean继承自DisposableBean或有自定义的销毁方法
	 *
	 * @param beanName
	 * @param bean
	 * @param beanDefinition
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition beanDefinition) {
		//只有singleton类型bean会执行销毁方法
		// 只有singleton的bean里的destroyMethodName会被注册成DisposableBean
		//若非singleton的bean，里面的destroyMethodName不会被注册成DisposableBean，其会在DisposableBeanAdapter里的destroy中的--2.---中执行？？？
		if (beanDefinition.isSingleton()) {
			//经典instanceof判断bean是否为工具bean~~~类似判断各种PostProcessor的bean~~~
			if (bean instanceof DisposableBean || StrUtil.isNotEmpty(beanDefinition.getDestroyMethodName())) {
				//Adapter适配器模式，用于适当的修改原接口，满足一定的需要。这里的话是为了增强原DisposableBean的功能~~原接口只提供了一个destroy方法，这里补充了beanDefinition等属性，可能有地方要用?
				registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, beanDefinition));
			}
		}
	}

	/**
	 * 实例化bean
	 *
	 * @param beanDefinition
	 * @return
	 */
	protected Object createBeanInstance(BeanDefinition beanDefinition) {
		return getInstantiationStrategy().instantiate(beanDefinition);
	}

	/**
	 * 为bean填充属性
	 *
	 * @param bean
	 * @param beanDefinition
	 */
	protected void applyPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
		try {
			for (PropertyValue propertyValue : beanDefinition.getPropertyValues().getPropertyValues()) {
				String name = propertyValue.getName();
				Object value = propertyValue.getValue();
				//如果属性是别的bean：
				if (value instanceof BeanReference) {
					// beanA依赖beanB，先实例化beanB
					BeanReference beanReference = (BeanReference) value;
					// 这个getBean:会先从单例三级缓存里找，如果没有的话那再从beanFactory里创建一个bean实例并返回-------<<<实质上就开始递归了
					value = getBean(beanReference.getBeanName()); //这里get的是实例化后的bean!（未被初始化也无所谓，后面由前面的递归子式完成初始化后，与这个value指向的是同一个对象）
				}
				//如果属性不是别的bean，则将value的类型通过反射从Object转化为定义好的的类型？
				else {
					//类型转换
					Class<?> sourceType = value.getClass();
					Class<?> targetType = (Class<?>) TypeUtil.getFieldType(bean.getClass(), name);
					ConversionService conversionService = getConversionService();
					if (conversionService != null) {
						if (conversionService.canConvert(sourceType, targetType)) {
							value = conversionService.convert(value, targetType);
						}
					}
				}

				//通过反射设置属性
				BeanUtil.setFieldValue(bean, name, value);
			}
		} catch (Exception ex) {
			throw new BeansException("Error setting property values for bean: " + beanName, ex);
		}
	}

	//设计思想:代理模式。即对初始的invokeInitMethod方法进行再次代理封装，在bean的实例化方法的前后加了2个Processer，用于在bean实例化前后进行自定义的操作
	protected Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
		if (bean instanceof BeanFactoryAware) {
			((BeanFactoryAware) bean).setBeanFactory(this);      //??感知这个BeanFactory
		}

		//执行BeanPostProcessor的前置处理
		Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

		try {
			invokeInitMethods(beanName, wrappedBean, beanDefinition);
		} catch (Throwable ex) {
			throw new BeansException("Invocation of init method of bean[" + beanName + "] failed", ex);
		}

		//执行BeanPostProcessor的后置处理 -----------------------若bean需要被代理，则将会在这一步（即在bean被初始化后）被代理！~~~~
		wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		return wrappedBean;
	}

	@Override
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {
		Object result = existingBean;
		//这里的BeanPostProcessors似乎是对所有的bean实例进行统一处理~~~
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			//这个地方调用的接口方法:processor.postProcessBeforeInitialization里到底用的是哪个实现类里的postProcessBeforeInitialization方法呢？
			//在于public List<BeanPostProcessor> getBeanPostProcessors()里已经有的实现类是哪一种类型！！！
			//见customTest~~~即只要注册到List里的实现类，就调用该实现类的方法即可~~~
			Object current = processor.postProcessBeforeInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	@Override
	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			Object current = processor.postProcessAfterInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	/**
	 * 执行bean的初始化方法
	 *
	 * @param beanName
	 * @param bean
	 * @param beanDefinition
	 * @throws Throwable
	 */
	protected void invokeInitMethods(String beanName, Object bean, BeanDefinition beanDefinition) throws Throwable {
		//-------1.先执行InitializingBean接口的afterPropertiesSet方法，见InitAndDestoryMethodTest class测试-------------------
		if (bean instanceof InitializingBean) {
			((InitializingBean) bean).afterPropertiesSet(); //----<<来自InitializingBean接口，即bean的初始化方法！
		}
		String initMethodName = beanDefinition.getInitMethodName();
		if (StrUtil.isNotEmpty(initMethodName)) {
		//-------2.再执行xml里注册好的beanDefinition里的initMethodName的方法--------------------
			Method initMethod = ClassUtil.getPublicMethod(beanDefinition.getBeanClass(), initMethodName);
			if (initMethod == null) {
				throw new BeansException("Could not find an init method named '" + initMethodName + "' on bean with name '" + beanName + "'");
			}
			initMethod.invoke(bean);
		}
	}

	public InstantiationStrategy getInstantiationStrategy() {
		return instantiationStrategy;
	}

	public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
		this.instantiationStrategy = instantiationStrategy;
	}
}
