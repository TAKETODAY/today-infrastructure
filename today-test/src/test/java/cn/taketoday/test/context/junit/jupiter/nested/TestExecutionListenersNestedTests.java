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

package cn.taketoday.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.ApplicationJUnitConfig;
import cn.taketoday.test.context.support.AbstractTestExecutionListener;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestExecutionListeners @TestExecutionListeners} in conjunction with the
 * {@link ApplicationExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@ApplicationJUnitConfig
@TestExecutionListeners(TestExecutionListenersNestedTests.FooTestExecutionListener.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TestExecutionListenersNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";
	private static final String QUX = "qux";

	private static final List<String> listeners = new ArrayList<>();


	@AfterEach
	void resetListeners() {
		listeners.clear();
	}

	@Test
	void test() {
		assertThat(listeners).containsExactly(FOO);
	}


	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedConfigTests {

		@Test
		void test() {
			assertThat(listeners).containsExactly(FOO);
		}
	}

	@Nested
	@ApplicationJUnitConfig(Config.class)
	@TestExecutionListeners(BarTestExecutionListener.class)
	class ConfigOverriddenByDefaultTests {

		@Test
		void test() {
			assertThat(listeners).containsExactly(BAR);
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	@ApplicationJUnitConfig(Config.class)
	@TestExecutionListeners(BarTestExecutionListener.class)
	class InheritedAndExtendedConfigTests {

		@Test
		void test() {
			assertThat(listeners).containsExactly(FOO, BAR);
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@ApplicationJUnitConfig(Config.class)
		@TestExecutionListeners(BazTestExecutionListener.class)
		class DoubleNestedWithOverriddenConfigTests {

			@Test
			void test() {
				assertThat(listeners).containsExactly(BAZ);
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			@TestExecutionListeners(listeners = BarTestExecutionListener.class, inheritListeners = false)
			class TripleNestedWithInheritedConfigButOverriddenListenersTests {

				@Test
				void test() {
					assertThat(listeners).containsExactly(BAR);
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Test
				void test() {
					assertThat(listeners).containsExactly(BAZ, QUX);
				}
			}
		}

	}

	// -------------------------------------------------------------------------

	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

	private static abstract class BaseTestExecutionListener extends AbstractTestExecutionListener {

		protected abstract String name();

		@Override
		public final void beforeTestClass(TestContext testContext) {
			listeners.add(name());
		}
	}

	static class FooTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return FOO;
		}
	}

	static class BarTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return BAR;
		}
	}

	static class BazTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return BAZ;
		}
	}

	static class QuxTestExecutionListener extends BaseTestExecutionListener {

		@Override
		protected String name() {
			return QUX;
		}
	}

	@TestExecutionListeners(QuxTestExecutionListener.class)
	interface TestInterface {
	}

}
