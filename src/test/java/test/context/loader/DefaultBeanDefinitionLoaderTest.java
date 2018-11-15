/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://yanghaijian.top
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
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
package test.context.loader;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.DefaultApplicationContext;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.BeanDefinitionLoader;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.demo.domain.User;

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

		try (ConfigurableApplicationContext applicationContext = new DefaultApplicationContext()) {

			BeanDefinitionRegistry registry = applicationContext.getEnvironment().getBeanDefinitionRegistry();
			
			BeanDefinitionLoader beanDefinitionLoader = applicationContext.getEnvironment().getBeanDefinitionLoader();
			
			beanDefinitionLoader.loadBeanDefinition(User.class); // will not register
			beanDefinitionLoader.loadBeanDefinition("user", User.class);
			
			Map<String, BeanDefinition> beanDefinitionsMap = registry.getBeanDefinitionsMap();

			assert beanDefinitionsMap.size() == 1;

			System.out.println(beanDefinitionsMap);
		}
	}

}
