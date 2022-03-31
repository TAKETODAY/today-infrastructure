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

package cn.taketoday.test.context.cache;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

/**
 * Integration test which verifies correct interaction between the
 * {@link DirtiesContextBeforeModesTestExecutionListener},
 * {@link DependencyInjectionTestExecutionListener}, and
 * {@link DirtiesContextTestExecutionListener} when
 * {@link DirtiesContext @DirtiesContext} is used at the method level.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MethodLevelDirtiesContextTests {

	private static final AtomicInteger contextCount = new AtomicInteger();


	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private Integer count;


	@Test
	@Order(1)
	void basics() throws Exception {
		performAssertions(1);
	}

	@Test
	@Order(2)
	@DirtiesContext(methodMode = BEFORE_METHOD)
	void dirtyContextBeforeTestMethod() throws Exception {
		performAssertions(2);
	}

	@Test
	@Order(3)
	@DirtiesContext
	void dirtyContextAfterTestMethod() throws Exception {
		performAssertions(2);
	}

	@Test
	@Order(4)
	void backToBasics() throws Exception {
		performAssertions(3);
	}

	private void performAssertions(int expectedContextCreationCount) throws Exception {
		assertThat(this.context).as("context must not be null").isNotNull();
		assertThat(this.context.isActive()).as("context must be active").isTrue();

		assertThat(this.count).as("count must not be null").isNotNull();
		assertThat(this.count.intValue()).as("count: ").isEqualTo(expectedContextCreationCount);

		assertThat(contextCount.get()).as("context creation count: ").isEqualTo(expectedContextCreationCount);
	}


	@Configuration
	static class Config {

		@Bean
		Integer count() {
			return contextCount.incrementAndGet();
		}
	}

}
