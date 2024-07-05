package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author derekyi
 * @date 2020/11/29
 */
public class InitAndDestoryMethodTest {

	@Test
	public void testInitAndDestroyMethod() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:init-and-destroy-method.xml");
		//为啥注册了就会调用destroy？？？-----会执行doClose方法，里面会调用对单例的destroy方法
		//还有，beanDefiniton里的destroyMethodName的destroy方法在哪执行的？
		//实现Disposable的destroy的方法是在哪执行的？
		//前两个destroy方法的先后执行顺序如何？---见DisposableBeanAdapter的destroy方法！！！~~~
		//如何判断一个bean实例到了需要destroy的阶段？----好像是ApplicationEvent这个接口管理各种事件
		applicationContext.registerShutdownHook();  //或者手动关闭 applicationContext.close();
	}
}
