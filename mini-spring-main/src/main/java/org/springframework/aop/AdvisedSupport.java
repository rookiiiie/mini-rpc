package org.springframework.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.AdvisorChainFactory;
import org.springframework.aop.framework.DefaultAdvisorChainFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zqc
 * @date 2022/12/16
 */
public class AdvisedSupport {

	//是否使用cglib代理
	private boolean proxyTargetClass = true;

	private TargetSource targetSource;


	private MethodMatcher methodMatcher;

	//用于存储方法的拦截器链，map<方法hasCode,拦截器advisor链>,在JDK动态代理方式下，拦截器advisor除了需要实现Advice，还需要实现MethodInterceptor接口的invoke方法自定义代理逻辑
	private transient Map<Integer, List<Object>> methodCache;

	//拦截器advisor链工厂
	AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

	private List<Advisor> advisors = new ArrayList<>();

	public AdvisedSupport() {
		this.methodCache = new ConcurrentHashMap<>(32);
	}
	public boolean isProxyTargetClass() {
		return proxyTargetClass;
	}

	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	public void addAdvisor(Advisor advisor) {
		advisors.add(advisor);
	}

	public List<Advisor> getAdvisors() {
		return advisors;
	}

	public TargetSource getTargetSource() {
		return targetSource;
	}

	public void setTargetSource(TargetSource targetSource) {
		this.targetSource = targetSource;
	}


	public MethodMatcher getMethodMatcher() {
		return methodMatcher;
	}

	public void setMethodMatcher(MethodMatcher methodMatcher) {
		this.methodMatcher = methodMatcher;
	}
	/**
	 * 用来返回方法的拦截器链---看不太懂咋写的？
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
		Integer cacheKey=method.hashCode(); //？？把method转化为一个什么hashCode？？
		List<Object> cached = this.methodCache.get(cacheKey);
		if (cached == null) {
			//这个方法会遍历AdvisedSupport里的所有advisor（这些advisor有些是同时实现了MethodInterceptor接口并实现PointcutAdvisor接口的，比如AspectJExpressionPointcutAdvisor这种advisor
			//所以可以通过判断advisor是否instanceof PointcutAdvisor来判断这个advisor是否是拦截器，收集对应method的拦截器链interceptorList
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
					this, method, targetClass);
			this.methodCache.put(cacheKey, cached);
		}
		return cached;
	}
}
