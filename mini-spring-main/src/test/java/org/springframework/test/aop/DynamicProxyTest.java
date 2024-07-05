package org.springframework.test.aop;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.AdvisedSupport;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor;
import org.springframework.aop.framework.CglibAopProxy;
import org.springframework.aop.framework.JdkDynamicAopProxy;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AfterReturningAdviceInterceptor;
import org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor;
import org.springframework.test.common.WorldServiceAfterReturnAdvice;
import org.springframework.test.common.WorldServiceBeforeAdvice;
import org.springframework.test.service.WorldService;
import org.springframework.test.service.WorldServiceImpl;

/**
 * @author derekyi
 * @date 2020/12/6
 */
public class DynamicProxyTest {

	private AdvisedSupport advisedSupport;

	@Before
	public void setup() {
		WorldService worldService = new WorldServiceImpl();

		advisedSupport = new ProxyFactory(); //ProxyFactory用于存储一些拦截器advisor~~~
		//Advisor是Pointcut和Advice的组合（既可以筛选需要被代理的某些方法，又可以对需要代理的方法进行代理操作)
		String expression = "execution(* org.springframework.test.service.WorldService.explode(..))";
		String expression2 = "execution(* org.springframework.test.service.WorldService.*(..))";
		AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();

		//配置advisor的expression（切点表达式，用于筛选需要被代理的方法）、methodInterceptorAdvisor（代理某个方法的具体逻辑）、targetSource（被代理的对象）
		advisor.setExpression(expression2);
		AfterReturningAdviceInterceptor methodInterceptor = new AfterReturningAdviceInterceptor(new WorldServiceAfterReturnAdvice());    //----方法拦截器--------
		advisor.setAdvice(methodInterceptor);  //AfterReturningAdviceInterceptor即是个Advice，又是个methodInterceptor。
		advisedSupport.setTargetSource(new TargetSource(worldService));
		advisedSupport.addAdvisor(advisor); //增加了一个拦截器advisor~~~
	}

	//JdkDynamicProxy中用的是org.aopalliance.intercept.MethodInvocation.invoke()里面的MethodInterceptor.intercept()来代理方法逻辑
	@Test
	public void testJdkDynamicProxy() throws Exception {
		WorldService proxy = (WorldService) new JdkDynamicAopProxy(advisedSupport).getProxy();
		proxy.explode();
		proxy.getName();
	}

	//CglibDynamicProxy中用的是MethodInterceptor.intercept()来代理方法逻辑
	@Test
	public void testCglibDynamicProxy() throws Exception {
		WorldService proxy = (WorldService) new CglibAopProxy(advisedSupport).getProxy();
		proxy.explode();
	}

	@Test
	public void testProxyFactory() throws Exception {
		// 使用JDK动态代理
		ProxyFactory factory = (ProxyFactory) advisedSupport;
		factory.setProxyTargetClass(false);
		WorldService proxy = (WorldService) factory.getProxy();
		proxy.explode();

		// 使用CGLIB动态代理
		factory.setProxyTargetClass(true);
		proxy = (WorldService) factory.getProxy();
		proxy.explode();
	}

	//这个beforeAdvice就是在实现MethodInterceptor的方法里面把修改逻辑放在invoke原方法前面了而已
	@Test
	public void testBeforeAdvice() throws Exception {
		//设置BeforeAdvice
		String expression = "execution(* org.springframework.test.service.WorldService.explode(..))";
		AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
		advisor.setExpression(expression);
		MethodBeforeAdviceInterceptor methodInterceptor = new MethodBeforeAdviceInterceptor(new WorldServiceBeforeAdvice());
		advisor.setAdvice(methodInterceptor);
		advisedSupport.addAdvisor(advisor);
		ProxyFactory factory = (ProxyFactory) advisedSupport;
		WorldService proxy = (WorldService) factory.getProxy();
		proxy.explode();
	}

	@Test
	public void testAdvisor() throws Exception {
		WorldService worldService = new WorldServiceImpl(); //被代理的对象

		//Advisor是Pointcut和Advice的组合
		String expression = "execution(* org.springframework.test.service.WorldService.explode(..))";
		AspectJExpressionPointcutAdvisor advisor = new AspectJExpressionPointcutAdvisor();
		advisor.setExpression(expression);
		MethodBeforeAdviceInterceptor methodInterceptor = new MethodBeforeAdviceInterceptor(new WorldServiceBeforeAdvice());
		advisor.setAdvice(methodInterceptor);

		ClassFilter classFilter = advisor.getPointcut().getClassFilter();
		if (classFilter.matches(worldService.getClass())) {
			ProxyFactory proxyFactory = new ProxyFactory();

			TargetSource targetSource = new TargetSource(worldService);
			proxyFactory.setTargetSource(targetSource);
			proxyFactory.addAdvisor(advisor); //添加拦截器
//			proxyFactory.setMethodMatcher(advisor.getPointcut().getMethodMatcher());
//			advisedSupport.setProxyTargetClass(true);   //JDK or CGLIB

			WorldService proxy = (WorldService) proxyFactory.getProxy();
			proxy.explode();
		}
	}
}
















