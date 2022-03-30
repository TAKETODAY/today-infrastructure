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
import org.junit.jupiter.api.TestInfo;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;
import cn.taketoday.test.context.junit4.nested.NestedTestsWithSpringRulesTests;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes in conjunction
 * with the {@link ApplicationExtension} in a JUnit Jupiter environment ... when using
 * constructor injection as opposed to field injection (see SPR-16653).
 *
 * <p>
 * To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0.5
 * @see ContextConfigurationNestedTests
 * @see NestedTestsWithSpringRulesTests
 */
@SpringJUnitConfig(ConstructorInjectionNestedTests.TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ConstructorInjectionNestedTests {

	final String foo;

	ConstructorInjectionNestedTests(TestInfo testInfo, @Autowired String foo) {
		this.foo = foo;
	}

	@Test
	void topLevelTest() {
		assertThat(foo).isEqualTo("foo");
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructorTests {

		final String bar;

		@Autowired
		AutowiredConstructorTests(String bar) {
			this.bar = bar;
		}

		@Test
		void nestedTest() throws Exception {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class AutowiredConstructorParameterTests {

		final String bar;

		AutowiredConstructorParameterTests(@Autowired String bar) {
			this.bar = bar;
		}

		@Test
		void nestedTest() throws Exception {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class QualifiedConstructorParameterTests {

		final String bar;

		QualifiedConstructorParameterTests(TestInfo testInfo, @Qualifier("bar") String s) {
			this.bar = s;
		}

		@Test
		void nestedTest() throws Exception {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
		}
	}

	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class SpelConstructorParameterTests {

		final String bar;
		final int answer;

		SpelConstructorParameterTests(@Autowired String bar, TestInfo testInfo, @Value("#{ 6 * 7 }") int answer) {
			this.bar = bar;
			this.answer = answer;
		}

		@Test
		void nestedTest() throws Exception {
			assertThat(foo).isEqualTo("foo");
			assertThat(bar).isEqualTo("bar");
			assertThat(answer).isEqualTo(42);
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class TopLevelConfig {

		@Bean
		String foo() {
			return "foo";
		}
	}

	@Configuration
	static class NestedConfig {

		@Bean
		String bar() {
			return "bar";
		}
	}

}
