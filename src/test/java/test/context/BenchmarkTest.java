package test.context;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import test.demo.config.Config;

/**
 * A simple benchmark test
 * 
 * @since 2.4
 */

public class BenchmarkTest {

	private long times = 5000;
//	private long times = 5000000;

	@Test
	public void testSingleton() {
		long start = System.currentTimeMillis();
		ApplicationContext applicationContext = new StandardApplicationContext();
		applicationContext.loadContext("");
		System.out.println("start context used: " + (System.currentTimeMillis() - start) + "ms");
		start = System.currentTimeMillis();
		for (int i = 0; i < times; i++) {
			applicationContext.getBean(Config.class);
//			applicationContext.getBean("Config");
		}
		long end = System.currentTimeMillis();
		applicationContext.close();
		System.out.println("Singleton used: " + (end - start) + "ms");
	}

//	@Test
	public void testPrototype() {
		long start = System.currentTimeMillis();
		ApplicationContext applicationContext = new StandardApplicationContext();
		applicationContext.loadContext("");
		System.out.println("start context used: " + (System.currentTimeMillis() - start) + "ms");
		start = System.currentTimeMillis();

		System.err.println(applicationContext.getBean("prototype_config"));
		for (int i = 0; i < times; i++) {
			applicationContext.getBean("prototype_config");
		}
		long end = System.currentTimeMillis();
		applicationContext.close();
		System.out.println("Prototype used: " + (end - start) + "ms");
	}

}
