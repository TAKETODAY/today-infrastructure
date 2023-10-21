/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.env;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for overriding properties from
 * properties files via inlined properties configured with
 * {@link TestPropertySource @TestPropertySource}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@TestPropertySource(properties = { "explicit = inlined", "extended = inlined1", "extended = inlined2" })
// Since ExplicitPropertiesFileTestPropertySourceTests is disabled in AOT mode, this class must be also.
@DisabledInAotMode
class MergedPropertiesFilesOverriddenByInlinedPropertiesTestPropertySourceTests extends
        MergedPropertiesFilesTestPropertySourceTests {

  @Test
  @Override
  void verifyPropertiesAreAvailableInEnvironment() {
    assertThat(env.getProperty("explicit")).isEqualTo("inlined");
  }

  @Test
  @Override
  void verifyExtendedPropertiesAreAvailableInEnvironment() {
    assertThat(env.getProperty("extended")).isEqualTo("inlined2");
  }

}
