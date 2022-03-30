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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.ApplicationJUnitConfig;
import cn.taketoday.test.context.junit4.nested.NestedTestsWithSpringRulesTests;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ContextConfiguration @ContextConfiguration} in conjunction with the
 * {@link ApplicationExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see ConstructorInjectionNestedTests
 * @see NestedTestsWithSpringRulesTests
 */
@ApplicationJUnitConfig(ContextConfigurationNestedTests.TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ContextConfigurationNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";

	@Autowired
	String foo;


	@Test
	void topLevelTest() {
		assertThat(foo).isEqualTo(FOO);
	}


	@Nested
	@ApplicationJUnitConfig(NestedConfig.class)
	class NestedTests {

		@Autowired(required = false)
		@Qualifier("foo")
		String localFoo;

		@Autowired
		String bar;


		@Test
		void test() {
			// In contrast to nested test classes running in JUnit 4, the foo
			// field in the outer instance should have been injected from the
			// test ApplicationContext for the outer instance.
			assertThat(foo).isEqualTo(FOO);
			assertThat(this.localFoo).as("foo bean should not be present").isNull();
			assertThat(this.bar).isEqualTo(BAR);
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	class NestedTestCaseWithInheritedConfigTests {

		@Autowired(required = false)
		@Qualifier("foo")
		String localFoo;

		@Autowired
		String bar;


		@Test
		void test() {
			// Since the configuration is inherited, the foo field in the outer instance
			// and the bar field in the inner instance should both have been injected
			// from the test ApplicationContext for the outer instance.
			assertThat(foo).isEqualTo(FOO);
			assertThat(this.localFoo).isEqualTo(FOO);
			assertThat(this.bar).isEqualTo(FOO);
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@ApplicationJUnitConfig(NestedConfig.class)
		class DoubleNestedWithOverriddenConfigTests {

			@Autowired(required = false)
			@Qualifier("foo")
			String localFoo;

			@Autowired
			String bar;


			@Test
			void test() {
				// In contrast to nested test classes running in JUnit 4, the foo
				// field in the outer instance should have been injected from the
				// test ApplicationContext for the outer instance.
				assertThat(foo).isEqualTo(FOO);
				assertThat(this.localFoo).as("foo bean should not be present").isNull();
				assertThat(this.bar).isEqualTo(BAR);
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigTests {

				@Autowired(required = false)
				@Qualifier("foo")
				String localFoo;

				@Autowired
				String bar;


				@Test
				void test() {
					assertThat(foo).isEqualTo(FOO);
					assertThat(this.localFoo).as("foo bean should not be present").isNull();
					assertThat(this.bar).isEqualTo(BAR);
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Autowired(required = false)
				@Qualifier("foo")
				String localFoo;

				@Autowired
				String bar;

				@Autowired
				String baz;


				@Test
				void test() {
					assertThat(foo).isEqualTo(FOO);
					assertThat(this.localFoo).as("foo bean should not be present").isNull();
					assertThat(this.bar).isEqualTo(BAR);
					assertThat(this.baz).isEqualTo(BAZ);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class TopLevelConfig {

		@Bean
		String foo() {
			return FOO;
		}
	}

	@Configuration
	static class NestedConfig {

		@Bean
		String bar() {
			return BAR;
		}
	}

	@Configuration
	static class TestInterfaceConfig {

		@Bean
		String baz() {
			return BAZ;
		}
	}

	@ContextConfiguration(classes = TestInterfaceConfig.class)
	interface TestInterface {
	}

}
