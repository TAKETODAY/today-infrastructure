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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import cn.taketoday.framework.web.servlet.mock.MockFilter;

import jakarta.servlet.Filter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link Filter} registration.
 *
 * @author Andy Wilkinson
 */
class FilterRegistrationIntegrationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void normalFiltersAreRegistered() {
		load(FilterConfiguration.class);
		assertThat(this.context.getServletContext().getFilterRegistrations()).hasSize(1);
	}

	@Test
	void scopedTargetFiltersAreNotRegistered() {
		load(ScopedTargetFilterConfiguration.class);
		assertThat(this.context.getServletContext().getFilterRegistrations()).isEmpty();
	}

	private void load(Class<?> configuration) {
		this.context = new AnnotationConfigServletWebServerApplicationContext(ContainerConfiguration.class,
				configuration);
	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfiguration {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ScopedTargetFilterConfiguration {

		@Bean(name = "scopedTarget.myFilter")
		Filter myFilter() {
			return new MockFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterConfiguration {

		@Bean
		Filter myFilter() {
			return new MockFilter();
		}

	}

}
