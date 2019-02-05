/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
package test.context.utils;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Today <br>
 *         2018-07-12 20:46:41
 */
public class ContextUtilsTest {

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
	public void test_FindInProperties() throws ConfigurationException {

		try (ApplicationContext applicationContext = new StandardApplicationContext("", "")) {

			Properties properties = applicationContext.getEnvironment().getProperties();

			properties.setProperty("Name", "#{siteName}");
			properties.setProperty("siteName", "#{site.name}");

			String name = ContextUtils.resolvePlaceholder(properties, "/#{Name}\\");
			String findInProperties = ContextUtils.resolvePlaceholder(properties, "/#{site.name}\\");
			String findInProperties_ = ContextUtils.resolvePlaceholder(properties, "/TODAY BLOG\\");

			assert findInProperties.equals(findInProperties_);
			assert findInProperties.equals(name);
			assert name.equals(findInProperties_);

			System.err.println(name);
			System.out.println(findInProperties);
			System.out.println(findInProperties_);
		}
	}

	@Test
	public void test_GetResourceAsStream() throws IOException {
		InputStream resourceAsStream = ContextUtils.getResourceAsStream("info.properties");

		assert resourceAsStream != null;
	}

	@Test
	public void test_GetResourceAsProperties() throws IOException {
		Properties resourceAsProperties = ContextUtils.getResourceAsProperties("info.properties");
		assert "TODAY BLOG".equals(resourceAsProperties.getProperty("site.name"));
	}

	@Test
	public void test_GetUrlAsStream() throws IOException {
		URL resource = ClassUtils.getClassLoader().getResource("info.properties");

		InputStream urlAsStream = ContextUtils.getUrlAsStream(resource.getProtocol() + ":" + resource.getPath());

		assert resource.getProtocol().equals("file");
		assert urlAsStream != null;
	}

	@Test
	public void test_GetUrlAsProperties() throws IOException {
		URL resource = ClassUtils.getClassLoader().getResource("info.properties");
		Properties properties = ContextUtils.getUrlAsProperties(resource.getProtocol() + ":" + resource.getPath());

		assert resource.getProtocol().equals("file");
		assert "TODAY BLOG".equals(properties.getProperty("site.name"));
	}

}
