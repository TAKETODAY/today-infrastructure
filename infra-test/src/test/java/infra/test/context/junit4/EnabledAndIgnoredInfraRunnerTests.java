/*
 * Copyright 2017 - 2026 the TODAY authors.
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
