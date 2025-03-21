/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core.env;

import org.junit.jupiter.api.Test;

import infra.beans.factory.config.PropertyPlaceholderConfigurer;
import infra.context.support.GenericApplicationContext;

import static infra.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

class PropertyPlaceholderConfigurerEnvironmentIntegrationTests {

	@Test
	@SuppressWarnings("deprecation")
	void test() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition("ppc",
				rootBeanDefinition(PropertyPlaceholderConfigurer.class)
				.addPropertyValue("searchSystemEnvironment", false)
				.getBeanDefinition());
		ctx.refresh();
		ctx.getBean("ppc");
		ctx.close();
	}

}
