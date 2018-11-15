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
package test.context.env;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.DefaultApplicationContext;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.env.StandardEnvironment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Today <br>
 * 
 *         2018-11-15 16:56
 */
public class StandardEnvironmentTest {

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
	public void test_AutoloadProperties()
			throws BeanDefinitionStoreException, NoSuchBeanDefinitionException, ConfigurationException {

		try (ApplicationContext applicationContext = new DefaultApplicationContext()) {
			Environment environment = applicationContext.getEnvironment();
			Properties properties = environment.getProperties();
			assert "https://taketoday.cn".equals(properties.getProperty("site.host"));
		}
	}

	@Test
	public void test_loadProperties() throws IOException {

		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.loadProperties(new File("src/test/resources")); // provide a path
		Properties properties = environment.getProperties();

		assert "https://taketoday.cn".equals(properties.getProperty("site.host"));
	}

	@Test
	public void test_ActiveProfile() throws IOException {

		try (ApplicationContext applicationContext = new DefaultApplicationContext()) {
			Environment environment = applicationContext.getEnvironment();

			String[] activeProfiles = environment.getActiveProfiles();
			for (String string : activeProfiles) {
				System.err.println(string);
			}
			assert "test".equals(activeProfiles[0]);
		}
	}

	@Test
	public void test_AddActiveProfile() throws IOException {

		try (ApplicationContext applicationContext = new DefaultApplicationContext()) {
			ConfigurableEnvironment environment = applicationContext.getEnvironment();

			environment.addActiveProfile("prod");
			String[] activeProfiles = environment.getActiveProfiles();
			assert activeProfiles.length == 3;
			assert "prod".equals(activeProfiles[2]);
		}
	}

	@Test
	public void test_AcceptsProfiles() throws IOException {

		try (ApplicationContext applicationContext = new DefaultApplicationContext()) {
			ConfigurableEnvironment environment = applicationContext.getEnvironment();

			assert environment.acceptsProfiles("test");
		}
	}

}
