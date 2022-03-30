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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.context.event.ApplicationEnvironmentPreparedEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.test.context.support.TestPropertySourceUtils;

/**
 * Tests for {@link DelegatingApplicationListener}.
 *
 * @author Dave Syer
 */
class DelegatingApplicationListenerTests {

	private final DelegatingApplicationListener listener = new DelegatingApplicationListener();

	private final StaticApplicationContext context = new StaticApplicationContext();

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void orderedInitialize() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"context.listener.classes=" + MockInitB.class.getName() + "," + MockInitA.class.getName());
		this.listener.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(new DefaultBootstrapContext(),
				new Application(), new String[0], this.context.getEnvironment()));
		this.context.getBeanFactory().registerSingleton("testListener", this.listener);
		this.context.refresh();
		assertThat(this.context.getBeanFactory().getSingleton("a")).isEqualTo("a");
		assertThat(this.context.getBeanFactory().getSingleton("b")).isEqualTo("b");
	}

	@Test
	void noInitializers() {
		this.listener.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(new DefaultBootstrapContext(),
				new Application(), new String[0], this.context.getEnvironment()));
	}

	@Test
	void emptyInitializers() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "context.listener.classes:");
		this.listener.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(new DefaultBootstrapContext(),
				new Application(), new String[0], this.context.getEnvironment()));
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class MockInitA implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event
					.getApplicationContext();
			applicationContext.getBeanFactory().registerSingleton("a", "a");
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	static class MockInitB implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event
					.getApplicationContext();
			assertThat(applicationContext.getBeanFactory().getSingleton("a")).isEqualTo("a");
			applicationContext.getBeanFactory().registerSingleton("b", "b");
		}

	}

}
