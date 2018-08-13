/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.test.context;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ClassPathApplicationContext;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.test.dao.impl.UserDaoImpl;
import cn.taketoday.test.domain.Config;
import cn.taketoday.test.domain.User;

/**
 * @author Today
 * @date 2018年7月3日 下午10:05:21
 */
public class ApplicationContextTest {

	private long start;

	@Before
	public void start() {
		start = System.currentTimeMillis();
	}

	@After
	public void end() {
		System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
	}

	/**
	 * test ApplicationContext
	 * 
	 * @throws NoSuchBeanDefinitionException
	 */
	@Test
	public void test_ApplicationContext() throws NoSuchBeanDefinitionException {

		ApplicationContext applicationContext = new ClassPathApplicationContext();
		applicationContext.loadContext("", "cn.taketoday.test.dao");
		Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getBeanDefinitionRegistry()
				.getBeanDefinitionsMap();

		System.out.println(beanDefinitionsMap);
		applicationContext.loadSuccess();

		boolean containsBean = applicationContext.containsBean(UserDaoImpl.class);

		applicationContext.close();

		assert containsBean : "UserDaoImpl load error.";
	}

	/**
	 * test auto load context and clear cache.
	 * 
	 * @throws NoSuchBeanDefinitionException
	 */
	@Test
	public void test_AutoLoadContextAndClearCache() throws NoSuchBeanDefinitionException {
		// auto load context and clear cache.
		ApplicationContext applicationContext = new ClassPathApplicationContext(true);

		Map<String, BeanDefinition> beanDefinitions = applicationContext.getBeanDefinitionRegistry()
				.getBeanDefinitionsMap();

		assert beanDefinitions.size() != 0 : "nothing in context.";

		Set<Class<?>> classCache = ClassUtils.getClassCache();

		assert classCache.size() == 0 : "cache clear error.";
		applicationContext.close();
	}

	/**
	 * test auto load context and get singleton.
	 * 
	 * @throws NoSuchBeanDefinitionException
	 */
	@Test
	public void test_AutoLoadSingleton() throws NoSuchBeanDefinitionException {
		ApplicationContext applicationContext = new ClassPathApplicationContext(true);
		Config config = applicationContext.getBean(Config.class);
		Config config_ = applicationContext.getBean(Config.class);

		assert config == config_ : "singleton error.";

		String copyright = config.getCopyright();

		assert copyright != null : "properties file load error.";

		applicationContext.close();
	}

	/**
	 * test auto load context and get prototype.
	 * 
	 * @throws NoSuchBeanDefinitionException
	 */
	@Test
	public void test_AutoLoadPrototype() throws NoSuchBeanDefinitionException {
		ApplicationContext applicationContext = new ClassPathApplicationContext(true);
		Config config = applicationContext.getBean("prototype_config", Config.class);
		Config config_ = applicationContext.getBean("prototype_config", Config.class);

		assert config != config_ : "prototype error.";

		applicationContext.close();
	}

public void test_AutoLoadFactoryBean_() throws NoSuchBeanDefinitionException {
	ApplicationContext applicationContext = new ClassPathApplicationContext(true);
	Config config = applicationContext.getBean("FactoryBean-Config", Config.class);
	Config config_ = applicationContext.getBean("FactoryBean-Config", Config.class);

	assert config == config_ : "FactoryBean error.";

	applicationContext.close();
}

	/**
	 * test auto load FactoryBean.
	 * 
	 * @throws NoSuchBeanDefinitionException
	 */
	@Test
	public void test_AutoLoadFactoryBean() throws NoSuchBeanDefinitionException {
		ApplicationContext applicationContext = new ClassPathApplicationContext(true);
		Config config = applicationContext.getBean("FactoryBean-Config", Config.class);
		Config config_ = applicationContext.getBean("FactoryBean-Config", Config.class);

		assert config == config_ : "FactoryBean error.";

		applicationContext.close();
	}

	/**
	 * Manual Loading.
	 * 
	 * @throws NoSuchBeanDefinitionException
	 * @throws BeanDefinitionStoreException
	 */
	@Test
	public void test_ManualLoadContext() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

		ApplicationContext applicationContext = new ClassPathApplicationContext();

		applicationContext.registerBean("user", User.class);
		applicationContext.registerBean("user_", User.class);
		applicationContext.onRefresh(); // init bean

		Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getBeanDefinitionRegistry()
				.getBeanDefinitionsMap();
		System.out.println(beanDefinitionsMap);

		Object bean = applicationContext.getBean("user");

		assert bean != null : "error";

		applicationContext.close();
	}

}
