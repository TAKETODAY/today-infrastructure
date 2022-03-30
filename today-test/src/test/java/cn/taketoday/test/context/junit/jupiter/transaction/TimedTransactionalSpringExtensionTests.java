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

package cn.taketoday.test.context.junit.jupiter.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.test.context.junit4.TimedTransactionalSpringRunnerTests;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.test.context.junit.jupiter.FailingTestCase;
import cn.taketoday.test.context.junit.jupiter.ApplicationJUnitConfig;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.Propagation;
import cn.taketoday.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * JUnit Jupiter based integration tests which verify support for Spring's
 * {@link Transactional @Transactional} annotation in conjunction with JUnit
 * Jupiter's {@link Timeout @Timeout}.
 *
 * @author Sam Brannen
 * @since 5.2
 * @see TimedTransactionalSpringRunnerTests
 */
class TimedTransactionalSpringExtensionTests {

	@Test
	void springTransactionsWorkWithJUnitJupiterTimeouts() {
		Events events = EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(TestCase.class))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(4).succeeded(2).failed(2));

		events.failed().assertThatEvents().haveExactly(2,
			event(test("WithExceededJUnitJupiterTimeout"),
				finishedWithFailure(
					instanceOf(TimeoutException.class),
					message(msg -> msg.endsWith("timed out after 10 milliseconds")))));
	}


	@ApplicationJUnitConfig
	@Transactional
	@FailingTestCase
	static class TestCase {

		@Test
		@Timeout(1)
		void transactionalWithJUnitJupiterTimeout() {
			TransactionAssert.assertThatTransaction().isActive();
		}

		@Test
		@Timeout(value = 10, unit = TimeUnit.MILLISECONDS)
		void transactionalWithExceededJUnitJupiterTimeout() throws Exception {
			TransactionAssert.assertThatTransaction().isActive();
			Thread.sleep(200);
		}

		@Test
		@Timeout(1)
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		void notTransactionalWithJUnitJupiterTimeout() {
			TransactionAssert.assertThatTransaction().isNotActive();
		}

		@Test
		@Timeout(value = 10, unit = TimeUnit.MILLISECONDS)
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		void notTransactionalWithExceededJUnitJupiterTimeout() throws Exception {
			TransactionAssert.assertThatTransaction().isNotActive();
			Thread.sleep(200);
		}


		@Configuration
		static class Config {

			@Bean
			PlatformTransactionManager transactionManager(DataSource dataSource) {
				return new DataSourceTransactionManager(dataSource);
			}

			@Bean
			DataSource dataSource() {
				return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
			}
		}

	}

}
