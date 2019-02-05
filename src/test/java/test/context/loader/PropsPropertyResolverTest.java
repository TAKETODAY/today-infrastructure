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
package test.context.loader;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.loader.PropsPropertyResolver;

import java.util.Properties;

import org.junit.Test;

/**
 * @author Today <br>
 * 
 *         2018-08-04 16:01
 */
public class PropsPropertyResolverTest {

	@Props(value = "info", prefix = "site")
	private Properties properties;

	@Props(value = "info", prefix = "site")
	private String name;

	@Test
	public void test_() throws Throwable {
		PropsPropertyResolver propertyResolver = new PropsPropertyResolver();

		try (ConfigurableApplicationContext applicationContext = new StandardApplicationContext()) {

			PropertyValue resolveProperty = //
					propertyResolver.resolveProperty(applicationContext, //
							PropsPropertyResolverTest.class.getDeclaredField("properties"));

			assert resolveProperty.getValue() != null;

			System.out.println("====================");
			System.out.println(resolveProperty.getValue());

		}
	}

	@Test
	public void test_Error() throws Throwable {
		PropsPropertyResolver propertyResolver = new PropsPropertyResolver();

		ApplicationContext applicationContext = new StandardApplicationContext();

		try {

			propertyResolver.resolveProperty(applicationContext, PropsPropertyResolverTest.class.getDeclaredField("name"));
		}
		catch (AnnotationException e) {
			System.err.println("AnnotationException");
		} finally {
			applicationContext.close();
		}

	}

}
