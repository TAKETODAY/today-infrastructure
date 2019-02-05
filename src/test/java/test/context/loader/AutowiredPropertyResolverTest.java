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

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.PropertyValueResolver;

import java.util.HashSet;

import org.junit.Test;

/**
 * @author Today <br>
 * 
 *         2018-08-04 15:56
 */
public class AutowiredPropertyResolverTest {

	@Autowired
	private String name;

	@Test
	public void test_() throws Throwable {

		PropertyValueResolver autowiredPropertyResolver = new AutowiredPropertyResolver();

		ConfigurableApplicationContext applicationContext = new StandardApplicationContext(new HashSet<>());

		applicationContext.getEnvironment().getBeanDefinitionLoader();

		PropertyValue resolveProperty = autowiredPropertyResolver.resolveProperty(applicationContext,
				AutowiredPropertyResolverTest.class.getDeclaredField("name"));

		System.err.println(resolveProperty);
		assert resolveProperty != null;

	}

}
