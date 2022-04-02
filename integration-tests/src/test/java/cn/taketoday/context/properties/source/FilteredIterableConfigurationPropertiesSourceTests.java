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

package cn.taketoday.context.properties.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredIterableConfigurationPropertiesSourceTests extends FilteredConfigurationPropertiesSourceTests {

  @Test
  void iteratorShouldFilterNames() {
    MockConfigurationPropertySource source = (MockConfigurationPropertySource) createTestSource();
    IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
    assertThat(filtered.iterator()).toIterable().extracting(ConfigurationPropertyName::toString)
            .containsExactly("a", "b", "c");
  }

  @Override
  protected ConfigurationPropertySource convertSource(MockConfigurationPropertySource source) {
    return source;
  }

  @Test
  void containsDescendantOfShouldUseContents() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar.baz", "1");
    source.put("foo.bar[0]", "1");
    source.put("faf.bar[0]", "1");
    IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
    assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
            .isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  private boolean noBrackets(ConfigurationPropertyName name) {
    return !name.toString().contains("[");
  }

}
