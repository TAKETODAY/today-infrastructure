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
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.TestConstructor;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.ApplicationJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static cn.taketoday.test.context.TestConstructor.AutowireMode.ALL;
import static cn.taketoday.test.context.TestConstructor.AutowireMode.ANNOTATED;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestConstructor @TestConstructor} in conjunction with the
 * {@link ApplicationExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@ApplicationJUnitConfig
@TestConstructor(autowireMode = ALL)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TestConstructorNestedTests {

	TestConstructorNestedTests(String text) {
		assertThat(text).isEqualTo("enigma");
	}

	@Test
	void test() {
	}


	@Nested
	@ApplicationJUnitConfig(Config.class)
	@TestConstructor(autowireMode = ANNOTATED)
	class ConfigOverriddenByDefaultTests {

		@Autowired
		ConfigOverriddenByDefaultTests(String text) {
			assertThat(text).isEqualTo("enigma");
		}

		@Test
		void test() {
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedConfigTests {

		InheritedConfigTests(String text) {
			assertThat(text).isEqualTo("enigma");
		}

		@Test
		void test() {
		}


		@Nested
		class DoubleNestedWithImplicitlyInheritedConfigTests {

			DoubleNestedWithImplicitlyInheritedConfigTests(String text) {
				assertThat(text).isEqualTo("enigma");
			}

			@Test
			void test() {
			}


			@Nested
			class TripleNestedWithImplicitlyInheritedConfigTests {

				TripleNestedWithImplicitlyInheritedConfigTests(String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}
			}
		}

		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@ApplicationJUnitConfig(Config.class)
		@TestConstructor(autowireMode = ANNOTATED)
		class DoubleNestedWithOverriddenConfigTests {

			DoubleNestedWithOverriddenConfigTests(@Autowired String text) {
				assertThat(text).isEqualTo("enigma");
			}

			@Test
			void test() {
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigTests {

				@Autowired
				TripleNestedWithInheritedConfigTests(String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				TripleNestedWithInheritedConfigAndTestInterfaceTests(String text) {
					assertThat(text).isEqualTo("enigma");
				}

				@Test
				void test() {
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class Config {

		@Bean
		String text() {
			return "enigma";
		}
	}

	@TestConstructor(autowireMode = ALL)
	interface TestInterface {
	}

}
