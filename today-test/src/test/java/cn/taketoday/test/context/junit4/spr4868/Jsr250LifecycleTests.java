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

package cn.taketoday.test.context.junit4.spr4868;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit4.ApplicationJUnit4ClassRunner;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that investigate the applicability of JSR-250 lifecycle
 * annotations in test classes.
 *
 * <p>This class does not really contain actual <em>tests</em> per se. Rather it
 * can be used to empirically verify the expected log output (see below). In
 * order to see the log output, one would naturally need to ensure that the
 * logger category for this class is enabled at {@code INFO} level.
 *
 * <h4>Expected Log Output</h4>
 * <pre>
 * INFO : cn.taketoday.test.context.junit4.spr4868.LifecycleBean - initializing
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - beforeAllTests()
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - setUp()
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - test1()
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - tearDown()
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - beforeAllTests()
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - setUp()
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - test2()
 * INFO : cn.taketoday.test.context.junit4.spr4868.ExampleTest - tearDown()
 * INFO : cn.taketoday.test.context.junit4.spr4868.LifecycleBean - destroying
 * </pre>
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(ApplicationJUnit4ClassRunner.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@ContextConfiguration
public class Jsr250LifecycleTests {

	private final Logger logger = LoggerFactory.getLogger(Jsr250LifecycleTests.class);


	@Configuration
	static class Config {

		@Bean
		public LifecycleBean lifecycleBean() {
			return new LifecycleBean();
		}
	}


	@Autowired
	private LifecycleBean lifecycleBean;


	@PostConstruct
	public void beforeAllTests() {
		logger.info("beforeAllTests()");
	}

	@PreDestroy
	public void afterTestSuite() {
		logger.info("afterTestSuite()");
	}

	@Before
	public void setUp() throws Exception {
		logger.info("setUp()");
	}

	@After
	public void tearDown() throws Exception {
		logger.info("tearDown()");
	}

	@Test
	public void test1() {
		logger.info("test1()");
		assertThat(lifecycleBean).isNotNull();
	}

	@Test
	public void test2() {
		logger.info("test2()");
		assertThat(lifecycleBean).isNotNull();
	}

}
