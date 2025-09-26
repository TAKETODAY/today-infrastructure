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

package infra.test.context.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link GenericXmlWebContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class GenericXmlWebContextLoaderTests {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  @Test
  void configMustNotContainAnnotatedClasses() throws Exception {
    GenericXmlWebContextLoader loader = new GenericXmlWebContextLoader();
    WebMergedContextConfiguration mergedConfig = new WebMergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
            new Class<?>[] { getClass() }, null, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY,
            "resource/path", loader, null, null);
    assertThatIllegalStateException()
            .isThrownBy(() -> loader.loadContext(mergedConfig))
            .withMessageContaining("does not support annotated classes");
  }

}
