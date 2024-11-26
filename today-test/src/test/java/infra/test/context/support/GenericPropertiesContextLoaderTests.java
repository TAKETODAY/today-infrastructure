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

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import infra.test.context.MergedContextConfiguration;
import infra.test.context.support.GenericPropertiesContextLoader;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link GenericPropertiesContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("deprecation")
class GenericPropertiesContextLoaderTests {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  @Test
  void configMustNotContainAnnotatedClasses() throws Exception {
    GenericPropertiesContextLoader loader = new GenericPropertiesContextLoader();
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            new Class<?>[] { getClass() }, EMPTY_STRING_ARRAY, loader);
    assertThatIllegalStateException()
            .isThrownBy(() -> loader.loadContext(mergedConfig))
            .withMessageContaining("does not support annotated classes");
  }

}
