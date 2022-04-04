/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ReactiveWebApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.WebApplicationContextRunner;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

/**
 * Tests for {@link ConditionalOnWarDeployment @ConditionalOnWarDeployment}.
 *
 * @author Madhura Bhave
 */
class ConditionalOnWarDeploymentTests {

	@Test
	void nonWebApplicationShouldNotMatch() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void reactiveWebApplicationShouldNotMatch() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void embeddedServletWebApplicationShouldNotMatch() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebApplicationContext::new);
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void warDeployedServletWebApplicationShouldMatch() {
		// sets a mock servletContext before context refresh which is what the
		// SpringBootServletInitializer does for WAR deployments.
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).hasBean("forWar"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWarDeployment
	static class TestConfiguration {

		@Bean
		String forWar() {
			return "forWar";
		}

	}

}
