package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;

/**
 * 后置增强拦截器
 *
 * @author zqc
 * @date 2022/12/20
 */
public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice {

	//在拦截器advisor里又添加了一个存放具体代理内容的advice
	private AfterReturningAdvice advice;

	public AfterReturningAdviceInterceptor() {
	}

	public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
		this.advice = advice;
	}


	//方法拦截器，即对方法的执行前后可以自己加逻辑，而这些逻辑的话用了AfterReturningAdvice接口的afterReturning方法~~~~
	//代理模式吧~~~
	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		Object retVal = mi.proceed();
		this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());         //---<<<代理对象的方法前后自己加东西
		return retVal;
	}
}
