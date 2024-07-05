package org.springframework.context.annotation;

import cn.hutool.core.util.ClassUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author derekyi
 * @date 2020/12/26
 *
 * //
 */
public class ClassPathScanningCandidateComponentProvider {

	//2024.6.23：增加Class T参数，在basePackage中扫描注解类型为T的bean
	public Set<BeanDefinition> findCandidateComponents(String basePackage,Class T) {
		Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
		// 扫描有org.springframework.stereotype.Component注解的类
		Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation(basePackage, T);
		for (Class<?> clazz : classes) {
			BeanDefinition beanDefinition = new BeanDefinition(clazz);
			candidates.add(beanDefinition);
		}
		return candidates;
	}
}
