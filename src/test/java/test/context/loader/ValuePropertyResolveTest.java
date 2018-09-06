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
package test.context.loader;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.DefaultApplicationContext;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.loader.ValuePropertyResolver;

/**
 * @author Today <br>
 * 
 *         2018-08-04 15:58
 */
public class ValuePropertyResolveTest {

	@Value("#{site.host}")
	private String host = null;

	@Test
	public void test_() throws Exception {

		ValuePropertyResolver propertyResolver = new ValuePropertyResolver();

		ApplicationContext applicationContext = new DefaultApplicationContext(true);

		BeanDefinitionRegistry registry = applicationContext.getBeanDefinitionRegistry();

		PropertyValue resolveProperty = propertyResolver.resolveProperty(registry,
				ValuePropertyResolveTest.class.getDeclaredField("host"));

		assert resolveProperty.getValue() != null;

		System.out.println("====================");
		System.out.println("Site -> " + resolveProperty.getValue());
		System.out.println("====================");
		applicationContext.close();

	}

}
