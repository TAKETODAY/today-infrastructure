/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.junit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import infra.test.annotation.IfProfileValue;
import infra.test.annotation.ProfileValueSource;
import infra.test.annotation.ProfileValueSourceConfiguration;
import infra.test.context.TestExecutionListeners;
import infra.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verifies proper handling of JUnit's {@link Ignore &#064;Ignore} and Infra
 * {@link IfProfileValue &#064;IfProfileValue} and
 * {@link ProfileValueSourceConfiguration &#064;ProfileValueSourceConfiguration}
 * (with the <em>implicit, default {@link ProfileValueSource}</em>) annotations in
 * conjunction with the {@link InfraRunner}.
 * <p>
 * Note that {@link TestExecutionListeners &#064;TestExecutionListeners} is
 * explicitly configured with an empty list, thus disabling all default
 * listeners.
 *
 * @author Sam Brannen
 * @see HardCodedProfileValueSourceInfraRunnerTests
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@TestExecutionListeners({})
// Since this test class does not load an ApplicationContext,
// this test class simply is not supported for AOT processing.
@DisabledInAotMode
public class EnabledAndIgnoredInfraRunnerTests {

  protected static final String NAME = "EnabledAndIgnoredInfraRunnerTests.profile_value.name";

  protected static final String VALUE = "enigma";

  protected static int numTestsExecuted = 0;

  @BeforeClass
  public static void setProfileValue() {
    numTestsExecuted = 0;
    System.setProperty(NAME, VALUE);
  }

  @AfterClass
  public static void verifyNumTestsExecuted() {
    assertThat(numTestsExecuted).as("Verifying the number of tests executed.").isEqualTo(3);
  }

  @Test
  @IfProfileValue(name = NAME, value = VALUE + "X")
  public void testIfProfileValueDisabled() {
    numTestsExecuted++;
    fail("The body of a disabled test should never be executed!");
  }

  @Test
  @IfProfileValue(name = NAME, value = VALUE)
  public void testIfProfileValueEnabledViaSingleValue() {
    numTestsExecuted++;
  }

  @Test
  @IfProfileValue(name = NAME, values = { "foo", VALUE, "bar" })
  public void testIfProfileValueEnabledViaMultipleValues() {
    numTestsExecuted++;
  }

  @Test
  public void testIfProfileValueNotConfigured() {
    numTestsExecuted++;
  }

  @Test
  @Ignore
  public void testJUnitIgnoreAnnotation() {
    numTestsExecuted++;
    fail("The body of an ignored test should never be executed!");
  }

}
