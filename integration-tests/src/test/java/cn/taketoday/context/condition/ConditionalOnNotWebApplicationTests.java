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
import cn.taketoday.framework.autoconfigure.web.reactive.MockReactiveWebServerFactory;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ReactiveWebApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.WebApplicationContextRunner;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.server.reactive.HttpHandler;

import cn.taketoday.context.condition.ConditionalOnNotWebApplication;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConditionalOnNotWebApplication @ConditionalOnNotWebApplication}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnNotWebApplicationTests {

	@Test
	void testNotWebApplicationWithServletContext() {
		new WebApplicationContextRunner().withUserConfiguration(NotWebApplicationConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void testNotWebApplicationWithReactiveContext() {
		new ReactiveWebApplicationContextRunner()
				.withUserConfiguration(ReactiveApplicationConfig.class, NotWebApplicationConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void testNotWebApplication() {
		new ApplicationContextRunner().withUserConfiguration(NotWebApplicationConfiguration.class)
				.run((context) -> assertThat(context).getBeans(String.class).containsExactly(entry("none", "none")));
	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveApplicationConfig {

		@Bean
		ReactiveWebServerFactory reactiveWebServerFactory() {
			return new MockReactiveWebServerFactory();
		}

		@Bean
		HttpHandler httpHandler() {
			return (request, response) -> Mono.empty();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnNotWebApplication
	static class NotWebApplicationConfiguration {

		@Bean
		String none() {
			return "none";
		}

	}

}
