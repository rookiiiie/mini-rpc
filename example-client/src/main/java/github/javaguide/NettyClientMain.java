package github.javaguide;

import github.javaguide.annotation.RpcScan;
//import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
//@RpcScan(basePackage = {"github.javaguide"})
public class NettyClientMain {
//    public static void main(String[] args) throws InterruptedException {
//        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyClientMain.class);
//        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
//        helloController.test();
//    }
    public static void main(String[] args) throws InterruptedException {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");

        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
        helloController.test();
    }
}
