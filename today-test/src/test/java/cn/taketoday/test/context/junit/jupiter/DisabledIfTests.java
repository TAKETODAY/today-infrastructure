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

package cn.taketoday.test.context.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests which verify support for {@link DisabledIf @DisabledIf}
 * in conjunction with the {@link ApplicationExtension} in a JUnit Jupiter environment.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Tadaya Tsuyukubo
 * @author Sam Brannen
 * @since 5.0
 * @see DisabledIfConditionTests
 * @see DisabledIf
 * @see ApplicationExtension
 */
class DisabledIfTests {

	@JUnitConfig(Config.class)
	@TestPropertySource(properties = "foo = true")
	@Nested
	class DisabledIfOnMethodTests {

		@Test
		@DisabledIf("true")
		void disabledIfWithStringTrue() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("   true   ")
		void disabledIfWithStringTrueWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("TrUe")
		void disabledIfWithStringTrueIgnoreCase() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("${__EnigmaPropertyShouldNotExist__:true}")
		void disabledIfWithPropertyPlaceholderForNonexistentPropertyWithDefaultValue() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf(expression = "${foo}", loadContext = true)
		void disabledIfWithPropertyPlaceholder() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf(expression = "\t${foo}   ", loadContext = true)
		void disabledIfWithPropertyPlaceholderWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{T(Boolean).TRUE}")
		void disabledIfWithSpelBoolean() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("   #{T(Boolean).TRUE}   ")
		void disabledIfWithSpelBooleanWithSurroundingWhitespace() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{'tr' + 'ue'}")
		void disabledIfWithSpelStringConcatenation() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("#{6 * 7 == 42}")
		void disabledIfWithSpelArithmeticComparison() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledOnMac
		void disabledIfWithSpelOsCheckInCustomComposedAnnotation() {
			String os = System.getProperty("os.name").toLowerCase();
			assertThat(os).as("This test must be disabled on Mac OS").doesNotContain("mac");
		}

		@Test
		@DisabledIf(expression = "#{@booleanTrueBean}", loadContext = true)
		void disabledIfWithSpelBooleanTrueBean() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf(expression = "#{@stringTrueBean}", loadContext = true)
		void disabledIfWithSpelStringTrueBean() {
			fail("This test must be disabled");
		}

	}

	@SpringJUnitConfig(Config.class)
	@Nested
	@DisabledIf("true")
	class DisabledIfOnClassTests {

		@Test
		void foo() {
			fail("This test must be disabled");
		}

		@Test
		@DisabledIf("false")
		void bar() {
			fail("This test must be disabled due to class-level condition");
		}

	}

	@Configuration
	static class Config {

		@Bean
		Boolean booleanTrueBean() {
			return Boolean.TRUE;
		}

		@Bean
		String stringTrueBean() {
			return "true";
		}
	}

}
