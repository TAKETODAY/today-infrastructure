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

import org.junit.BeforeClass;

import infra.test.annotation.IfProfileValue;
import infra.test.annotation.ProfileValueSource;
import infra.test.annotation.ProfileValueSourceConfiguration;
import infra.test.context.aot.DisabledInAotMode;

/**
 * <p>
 * Verifies proper handling of JUnit's {@link org.junit.Ignore &#064;Ignore} and
 * Infra {@link IfProfileValue
 * &#064;IfProfileValue} and {@link ProfileValueSourceConfiguration
 * &#064;ProfileValueSourceConfiguration} (with an
 * <em>explicit, custom defined {@link ProfileValueSource}</em>) annotations in
 * conjunction with the {@link InfraRunner}.
 * </p>
 *
 * @author Sam Brannen
 * @see EnabledAndIgnoredInfraRunnerTests
 * @since 4.0
 */
@ProfileValueSourceConfiguration(HardCodedProfileValueSourceInfraRunnerTests.HardCodedProfileValueSource.class)
// Since EnabledAndIgnoredSpringRunnerTests is disabled in AOT mode, this test class must be also.
@DisabledInAotMode
public class HardCodedProfileValueSourceInfraRunnerTests extends EnabledAndIgnoredInfraRunnerTests {

  @BeforeClass
  public static void setProfileValue() {
    numTestsExecuted = 0;
    // Set the system property to something other than VALUE as a sanity
    // check.
    System.setProperty(NAME, "999999999999");
  }

  public static class HardCodedProfileValueSource implements ProfileValueSource {

    @Override
    public String get(final String key) {
      return (key.equals(NAME) ? VALUE : null);
    }
  }
}
