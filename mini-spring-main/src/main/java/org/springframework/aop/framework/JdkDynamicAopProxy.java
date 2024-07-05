package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.AdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * JDK动态代理
 *
 * @author zqc
 * @date 2022/12/19
 */

// InvocationHandler这种反射里面的句柄接口，是实际执行这个类里的方式的时候，暴露出来的一段执行逻辑给用户来自定义
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {

	private final AdvisedSupport advised;

	public JdkDynamicAopProxy(AdvisedSupport advised) {
		this.advised = advised;
	}

	/**
	 * 返回代理对象
	 * jDK代理是基于接口的。
	 *
	 * @return
	 */
	@Override
	public Object getProxy() {
		/**
		 * public static Object newProxyInstance(
		 * 	   ClassLoader loader,          ---<<一个classloader对象，定义了由哪个classloader对象对生成的代理类进行加载
		 *     @NotNull Class<?>[] interfaces,   --<<一个interface数组，表示我们将要给我们的代理对象提供一组什么样的接口，如果我们提供了这样一个接口对象数组，那么也就是声明了代理类实现了这些接口，代理类就可以调用接口中声明的所有方法。在这里，直接targetClass.getInterfaces()
		 *     @NotNull reflect.InvocationHandler h --<<一个InvocationHandler对象，表示的是当动态代理对象调用方法的时候会关联到哪一个InvocationHandler对象上，并最终由其调用。
		 * )
		 */
		return Proxy.newProxyInstance(getClass().getClassLoader(), advised.getTargetSource().getTargetClass(), this);
	}

	//----这个invoke，针对的是被代理对象的所有方法么？？？---------------感觉要去看看Proxy.newProxyInstance(),虽然看不懂= =.
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 获取目标对象
		Object target = advised.getTargetSource().getTarget(); //target对象为原来需要被代理的对象~~~
		Class<?> targetClass = target.getClass(); //反射动态获取类的信息
		Object retVal = null;
		// 获取当前方法method对应的拦截器链，通过Map<Integer, List<Object>> methodCache结构存储该方法对应的拦截器链。
		List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
		if (chain == null || chain.isEmpty()) { //如果没代理拦截器，则执行原对象的方法就行，不需要代理~~~
			return method.invoke(target, args);
		} else {
			// 将拦截器统一封装成ReflectiveMethodInvocation-------------------这样的话可以在拦截器advisor的invoke方法里代理原对象的方法(在)
			MethodInvocation invocation =
					new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain); //利用反射实现的方法代理调用
			// Proceed to the joinpoint through the interceptor chain.
			// 执行拦截器链
			retVal = invocation.proceed();   //----------这个里面会执行实现了MethodInterceptor接口方法的方法拦截器实体类的invoke方法，可以在这个invoke方法里面进行代理操作~~~
		}
		return retVal;
	}
}
