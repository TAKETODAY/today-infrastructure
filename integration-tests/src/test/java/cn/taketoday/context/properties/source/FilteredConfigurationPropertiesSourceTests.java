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
import org.mockito.Answers;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredConfigurationPropertiesSourceTests {

  @Test
  void createWhenSourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new FilteredConfigurationPropertiesSource(null, Objects::nonNull))
            .withMessageContaining("Source must not be null");
  }

  @Test
  void createWhenFilterIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(
                    () -> new FilteredConfigurationPropertiesSource(new MockConfigurationPropertySource(), null))
            .withMessageContaining("Filter must not be null");
  }

  @Test
  void getValueShouldFilterNames() {
    ConfigurationPropertySource source = createTestSource();
    ConfigurationPropertySource filtered = source.filter(this::noBrackets);
    ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
    assertThat(source.getConfigurationProperty(name).getValue()).isEqualTo("1");
    assertThat(filtered.getConfigurationProperty(name).getValue()).isEqualTo("1");
    ConfigurationPropertyName bracketName = ConfigurationPropertyName.of("a[1]");
    assertThat(source.getConfigurationProperty(bracketName).getValue()).isEqualTo("2");
    assertThat(filtered.getConfigurationProperty(bracketName)).isNull();
  }

  @Test
  void containsDescendantOfWhenSourceReturnsEmptyShouldReturnEmpty() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.UNKNOWN);
    ConfigurationPropertySource filtered = source.filter((n) -> true);
    assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.UNKNOWN);
  }

  @Test
  void containsDescendantOfWhenSourceReturnsFalseShouldReturnFalse() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
    ConfigurationPropertySource filtered = source.filter((n) -> true);
    assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  @Test
  void containsDescendantOfWhenSourceReturnsTrueShouldReturnEmpty() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.PRESENT);
    ConfigurationPropertySource filtered = source.filter((n) -> true);
    assertThat(filtered.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.UNKNOWN);
  }

  protected final ConfigurationPropertySource createTestSource() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("a", "1");
    source.put("a[1]", "2");
    source.put("b", "3");
    source.put("b[1]", "4");
    source.put("c", "5");
    return convertSource(source);
  }

  protected ConfigurationPropertySource convertSource(MockConfigurationPropertySource source) {
    return source.nonIterable();
  }

  private boolean noBrackets(ConfigurationPropertyName name) {
    return !name.toString().contains("[");
  }

}
