package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.AdvisedSupport;
import org.springframework.aop.Advisor;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zqc
 * @date 2022/12/17
 */
public class DefaultAdvisorChainFactory implements AdvisorChainFactory {

	//这个方法会遍历AdvisedSupport里的所有advisor（这些advisor有些是同时实现了MethodInterceptor接口并实现PointcutAdvisor接口的，比如AspectJExpressionPointcutAdvisor这种advisor)
	//所以可以通过判断advisor是否instanceof PointcutAdvisor来判断这个advisor是否是拦截器，收集对应method的拦截器链interceptorList
	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(AdvisedSupport config, Method method, Class<?> targetClass) {
		Advisor[] advisors = config.getAdvisors().toArray(new Advisor[0]);
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor) {  //判断是否是实现了MethodInterceptor接口并实现PointcutAdvisor接口的作为方法的拦截器使用的advisor
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				// 校验当前Advisor是否适用于当前对象
				if (pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					// 校验Advisor是否应用到当前方法上
					match = mm.matches(method, actualClass);
					if (match) {
						MethodInterceptor interceptor = (MethodInterceptor) advisor.getAdvice();
						interceptorList.add(interceptor);
					}
				}
			}
		}
		return interceptorList;
	}
}
