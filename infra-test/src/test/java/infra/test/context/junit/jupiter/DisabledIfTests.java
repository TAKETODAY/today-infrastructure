/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests which verify support for {@link DisabledIf @DisabledIf}
 * in conjunction with the {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * @author Tadaya Tsuyukubo
 * @author Sam Brannen
 * @see DisabledIfConditionTests
 * @see DisabledIf
 * @see InfraExtension
 * @since 4.0
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

  @JUnitConfig(Config.class)
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
