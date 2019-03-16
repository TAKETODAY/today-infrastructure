/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://yanghaijian.top
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.context.loader;

import java.util.HashSet;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.DefaultBeanNameCreator;
import cn.taketoday.context.env.StandardEnvironment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.DefaultBeanDefinitionLoader;
import test.demo.config.User;

/**
 * Default Bean Definition Loader implements
 * 
 * @author Today <br>
 *         2018-06-23 11:18:22
 */
public final class DefaultBeanDefinitionLoaderTest {

	private long start;

	@Before
	public void start() {

		start = System.currentTimeMillis();
	}

	@After
	public void end() {
		System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
	}

	@Test
	public void test_LoadBeanDefinition() throws BeanDefinitionStoreException, ConfigurationException {

		try (ConfigurableApplicationContext applicationContext = new StandardApplicationContext(new HashSet<>())) {

			BeanDefinitionRegistry registry = applicationContext.getEnvironment().getBeanDefinitionRegistry();

			BeanDefinitionLoader beanDefinitionLoader = applicationContext.getEnvironment().getBeanDefinitionLoader();

			beanDefinitionLoader.loadBeanDefinition(User.class); // will not register
			beanDefinitionLoader.loadBeanDefinition("user", User.class);

			Map<String, BeanDefinition> beanDefinitionsMap = registry.getBeanDefinitionsMap();

			assert beanDefinitionsMap.size() == 1;

			System.out.println(beanDefinitionsMap);

			User user = applicationContext.getBeanFactory().getBean(User.class);
			User bean = applicationContext.getBean(User.class);
			assert user == bean;
			System.err.println(bean);
		}
	}

	@Test
	public void test_createBeanDefinition() {

		try (ConfigurableApplicationContext applicationContext = new StandardApplicationContext()) {

			ConfigurableEnvironment environment = new StandardEnvironment();
			DefaultBeanNameCreator beanNameCreator = new DefaultBeanNameCreator(environment);

			environment.setBeanDefinitionRegistry(applicationContext);
			environment.setBeanNameCreator(beanNameCreator);

			applicationContext.setEnvironment(environment);

			BeanDefinitionLoader beanDefinitionLoader = new DefaultBeanDefinitionLoader(applicationContext);
			BeanDefinition createBeanDefinition = beanDefinitionLoader.createBeanDefinition(User.class);
			System.err.println(createBeanDefinition);

			assert createBeanDefinition.getBeanClass() == User.class;
			assert createBeanDefinition.getName().equals(beanNameCreator.create(createBeanDefinition.getBeanClass()));
			assert createBeanDefinition.getScope() == Scope.SINGLETON;
			assert !createBeanDefinition.isAbstract();
			assert !createBeanDefinition.isFactoryBean();
			assert !createBeanDefinition.isInitialized();
		}
	}

}
