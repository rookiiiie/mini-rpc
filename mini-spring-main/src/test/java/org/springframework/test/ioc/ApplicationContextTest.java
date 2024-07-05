package org.springframework.test.ioc;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.bean.Car;
import org.springframework.test.bean.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author derekyi
 * @date 2020/11/28
 */
public class ApplicationContextTest {

	@Test
	public void testApplicationContext() throws Exception {
		//这个ClassPathXmlApplicationContext继承自AbstractApplicationContext，初始化完后就会调用AbstractApplicationContext的refresh从配置文件里导入beanDefinition并实例化非懒加载的单例并且会添加一堆Processor的玩意

		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");

		Person person = applicationContext.getBean("person", Person.class);
		System.out.println(person);
		//name属性在CustomBeanFactoryPostProcessor中被修改为ivy
		assertThat(person.getName()).isEqualTo("ivy");

		Car car = applicationContext.getBean("car", Car.class);
		System.out.println(car);
		//brand属性在CustomerBeanPostProcessor中被修改为lamborghini
		assertThat(car.getBrand()).isEqualTo("lamborghini");
	}
}
