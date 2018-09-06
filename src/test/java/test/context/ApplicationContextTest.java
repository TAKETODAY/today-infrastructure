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
package test.context;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.AnntationApplicationContext;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.DefaultApplicationContext;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ClassUtils;
import test.dao.UserDao;
import test.dao.impl.UserDaoImpl;
import test.domain.Config;
import test.domain.ConfigurationBean;
import test.domain.User;
import test.service.UserService;
import test.service.impl.UserServiceImpl;

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

		ApplicationContext applicationContext = new DefaultApplicationContext();
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
		ApplicationContext applicationContext = new DefaultApplicationContext(true);

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
		ApplicationContext applicationContext = new DefaultApplicationContext(true);
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
		ApplicationContext applicationContext = new DefaultApplicationContext(true);
		Config config = applicationContext.getBean("prototype_config", Config.class);
		Config config_ = applicationContext.getBean("prototype_config", Config.class);

		assert config != config_ : "prototype error.";

		applicationContext.close();
	}

	public void test_AutoLoadFactoryBean_() throws NoSuchBeanDefinitionException {
		ApplicationContext applicationContext = new DefaultApplicationContext(true);
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
		ApplicationContext applicationContext = new DefaultApplicationContext(true);
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

		ApplicationContext applicationContext = new DefaultApplicationContext();

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

	@Test
	public void test_Login() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

		ApplicationContext applicationContext = new DefaultApplicationContext(false);

		UserService userService = applicationContext.getBean(UserServiceImpl.class);

		UserDao userDao = applicationContext.getBean(UserDao.class);
		UserDaoImpl userDaoImpl = applicationContext.getBean(UserDaoImpl.class);

		assert userDao == userDaoImpl;

		User login = userService.login(new User(1, "TODAY", 20, "666", "666", "男", new Date()));

		assert login != null : "Login failed";

		applicationContext.close();
	}

	@Test
	public void test_AnntationApplicationContext() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

		ApplicationContext applicationContext = new AnntationApplicationContext(
				new HashSet<>(Arrays.asList(ConfigurationBean.class)));

		long start = System.currentTimeMillis();

		User bean = applicationContext.getBean("user", User.class);
		System.err.println(System.currentTimeMillis() - start + "ms");
		System.err.println(applicationContext.getBeanDefinitionRegistry().getBeanDefinitionsMap());

		bean.setAge(12);

		System.err.println(bean);

		User user = applicationContext.getBean("user", User.class);

		assert bean != user;
		
		System.err.println(user);
		applicationContext.close();
	}

}
