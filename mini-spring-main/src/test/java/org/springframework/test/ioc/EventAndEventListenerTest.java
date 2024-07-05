package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.common.event.CustomEvent;

/**
 * @author derekyi
 * @date 2020/12/5
 */
public class EventAndEventListenerTest {

	@Test
	public void testEventListener() throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:event-and-event-listener.xml");
		applicationContext.publishEvent(new CustomEvent(applicationContext));

		//为了确保销毁方法在虚拟机关闭之前执行，向虚拟机中注册一个钩子方法
		applicationContext.registerShutdownHook();//或者applicationContext.close()主动关闭容器;
	}
}
